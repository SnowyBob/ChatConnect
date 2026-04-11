const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendChatNotification = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const chatId = context.params.chatId;

    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    if (!chatDoc.exists) return null;

    const chat = chatDoc.data();
    const participants = chat.participants || [];
    const chatName = chat.name || "";
    const senderId = message.senderId;
    const senderName = message.senderName || "Someone";
    const messageText = message.text || "";

    const tokens = [];
    for (const uid of participants) {
      if (uid === senderId) continue;
      const userDoc = await admin.firestore().collection("users").doc(uid).get();
      if (userDoc.exists && userDoc.data().fcmToken) {
        tokens.push(userDoc.data().fcmToken);
      }
    }

    if (tokens.length === 0) return null;

    const response = await admin.messaging().sendEachForMulticast({
      tokens: tokens,
      data: {
        senderName: senderName,
        messageText: messageText,
        chatId: chatId,
        chatName: chatName,
      },
    });

    console.log("Sent:", response.successCount, "Failed:", response.failureCount);
    return null;
  });