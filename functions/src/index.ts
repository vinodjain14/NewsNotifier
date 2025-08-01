import {onSchedule} from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as logger from "firebase-functions/logger";
import Parser from "rss-parser";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();
const rssParser = new Parser();

interface Article {
  title: string;
  url: string;
  sourceName: string;
  contentSnippet: string;
  publishTimestamp: admin.firestore.Timestamp;
  fetchedTimestamp: admin.firestore.Timestamp;
  isBreaking: boolean;
}

export const fetchAndNotify = onSchedule(
  {
    schedule: "every 15 minutes",
    memory: "512MiB",
    timeoutSeconds: 300,
  },
  async () => {
    try {
      logger.info("Starting RSS feed fetch cycle.");

      try {
        const subscriptionsSnapshot = await db.collectionGroup("subscriptions")
          .where("type", "==", "RSS_FEED")
          .get();

        if (subscriptionsSnapshot.empty) {
          logger.info("No active RSS subscriptions found. Exiting.");
          return;
        }

        const uniqueUrls = new Set<string>();
        subscriptionsSnapshot.forEach((doc) => {
          const data = doc.data();
          if (data.sourceUrl) {
            uniqueUrls.add(data.sourceUrl);
          }
        });

        logger.info(
          `Found ${uniqueUrls.size} unique URLs to fetch.`
        );

        const fetchTasks = Array.from(uniqueUrls).map(async (url) => {
          try {
            logger.info(`Fetching RSS feed: ${url}`);
            const feed = await rssParser.parseURL(url);
            if (!feed.items?.length) return;

            for (const item of feed.items) {
              const guid = item.guid || item.link;
              if (!guid || !item.title || !item.link) continue;

              const articleId = crypto
                .createHash("sha256")
                .update(guid)
                .digest("hex");

              const articleRef = db
                .collection("articles")
                .doc(articleId);

              const articleDoc = await articleRef.get();
              if (articleDoc.exists) continue;

              logger.info(
                `New article: ${item.title}`
              );

              let publishTimestamp: admin.firestore.Timestamp;
              try {
                publishTimestamp = item.isoDate ?
                  admin.firestore.Timestamp.fromDate(
                    new Date(item.isoDate)
                  ) :
                  admin.firestore.Timestamp.now();
              } catch {
                publishTimestamp = admin.firestore.Timestamp.now();
              }

              const newArticle: Article = {
                title: item.title,
                url: item.link,
                sourceName: feed.title || "Unknown Source",
                contentSnippet: item.contentSnippet ||
                item.content || "",
                publishTimestamp,
                fetchedTimestamp: admin.firestore.Timestamp.now(),
                isBreaking: item.title
                  .toLowerCase()
                  .includes("breaking"),
              };

              await articleRef.set(newArticle);
              await sendNotificationsForArticle(
                url,
                newArticle,
                articleRef
              );
            }
          } catch (err) {
            logger.error(`Error processing feed ${url}`, err);
          }
        });

        await Promise.all(fetchTasks);
        logger.info("RSS fetch cycle complete.");
      } catch (e) {
        logger.error("Failed Firestore query for subscriptions:", e);
        throw e; // so you see it in logs clearly
      }
    } catch (err) {
      logger.error("Fatal Error");
      throw err;
    }
  }
);

/**
 * Sends notifications to users subscribed to a feed and logs them.
 *
 * @param {string} feedUrl The RSS feed URL.
 * @param {Article} article The new article object.
 * @param {FirebaseFirestore.DocumentReference} articleRef Firestore ref.
 */
async function sendNotificationsForArticle(
  feedUrl: string,
  article: Article,
  articleRef: FirebaseFirestore.DocumentReference
) {
  try {
    const subsSnapshot = await db
      .collectionGroup("subscriptions")
      .where("sourceUrl", "==", feedUrl)
      .get();

    if (subsSnapshot.empty) return;

    const userIds = subsSnapshot.docs
      .map((doc) =>
        doc.ref.parent.parent ?
          doc.ref.parent.parent.id :
          null
      )
      .filter((id): id is string => id !== null);

    const tokens: string[] = [];

    await Promise.all(
      userIds.map(async (userId) => {
        try {
          const userDoc = await db
            .collection("users")
            .doc(userId)
            .get();

          const userData = userDoc.data();
          if (
            userData?.fcmTokens &&
            Array.isArray(userData.fcmTokens)
          ) {
            tokens.push(...userData.fcmTokens);
          }

          await db
            .collection("users")
            .doc(userId)
            .collection("notifications")
            .add({
              title: article.title,
              message: article.contentSnippet.substring(0, 100),
              sourceName: article.sourceName,
              timestamp: admin.firestore.FieldValue.serverTimestamp(),
              isRead: false,
              articleRef,
            });
        } catch (userError) {
          logger.error(
            `Error processing user ${userId}:`,
            userError
          );
        }
      })
    );

    if (tokens.length > 0) {
      const payload: admin.messaging.MessagingPayload = {
        notification: {
          title: `New from ${article.sourceName}`,
          body: article.title,
          sound: "default",
        },
        data: {url: article.url},
      };

      logger.info(
        `Sending notification for "${article.title}" ` +
        `to ${tokens.length} tokens.`
      );

      try {
        await messaging.sendToDevice(tokens, payload);
      } catch (msgError) {
        logger.error(
          "Failed to send push notification:",
          msgError
        );
      }
    }
  } catch (err) {
    logger.error("Error in sendNotificationsForArticle:", err);
  }
}
