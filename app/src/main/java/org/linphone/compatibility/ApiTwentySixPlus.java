package org.linphone.compatibility;


import android.annotation.TargetApi;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.ViewTreeObserver;

import org.linphone.R;
import org.linphone.mediastream.Log;
import org.linphone.receivers.NotificationBroadcastReceiver;

import static org.linphone.compatibility.Compatibility.INTENT_NOTIF_ID;
import static org.linphone.compatibility.Compatibility.KEY_TEXT_REPLY;

/*
ApiTwentySixPlus.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

@TargetApi(26)
public class ApiTwentySixPlus {
	public static Notification createRepliedNotification(Context context, String reply) {
		Notification repliedNotification = new Notification.Builder(context, context.getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.topbar_chat_notification)
            .setContentText(context.getString(R.string.notification_replied_label).replace("%s", reply))
            .build();

		return repliedNotification;
	}

	public static void createServiceChannel(Context context) {
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Create service/call notification channel
		String id = context.getString(R.string.notification_service_channel_id);
		CharSequence name = context.getString(R.string.content_title_notification_service);
		String description = context.getString(R.string.content_title_notification_service);
		NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_NONE);
		channel.setDescription(description);
		channel.enableVibration(false);
		channel.enableLights(false);
        channel.setShowBadge(false);
		notificationManager.createNotificationChannel(channel);
	}

    public static void createMessageChannel(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Create message notification channel
        String id = context.getString(R.string.notification_channel_id);
        String name = context.getString(R.string.content_title_notification);
        String description = context.getString(R.string.content_title_notification);
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);
        channel.setLightColor(context.getColor(R.color.notification_color_led));
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setShowBadge(true);
        notificationManager.createNotificationChannel(channel);
    }

	public static Notification createMessageNotification(Context context, int notificationId, int msgCount, String msgSender, String msg, Bitmap contactIcon, PendingIntent intent) {
		String title;
		if (msgCount == 1) {
			title = msgSender;
		} else {
			title = context.getString(R.string.unread_messages).replace("%i", String.valueOf(msgCount));
		}

		String replyLabel = context.getResources().getString(R.string.notification_reply_label);
		RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY).setLabel(replyLabel).build();

		Intent replyIntent = new Intent(context, NotificationBroadcastReceiver.class);
		replyIntent.setAction(context.getPackageName() + ".REPLY_ACTION");
		replyIntent.putExtra(INTENT_NOTIF_ID, notificationId);

		PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context,
            notificationId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Action action = new Notification.Action.Builder(R.drawable.chat_send_over,
            context.getString(R.string.notification_reply_label), replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build();

		Notification notif;
		notif = new Notification.Builder(context, context.getString(R.string.notification_channel_id))
			.setContentTitle(title)
			.setContentText(msg)
			.setSmallIcon(R.drawable.topbar_chat_notification)
			.setAutoCancel(true)
			.setContentIntent(intent)
			.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
			.setLargeIcon(contactIcon)
			.setCategory(Notification.CATEGORY_MESSAGE)
			.setVisibility(Notification.VISIBILITY_PRIVATE)
			.setPriority(Notification.PRIORITY_HIGH)
			.setNumber(msgCount)
			.setWhen(System.currentTimeMillis())
			.setShowWhen(true)
			.setColor(context.getColor(R.color.notification_color_led))
			.addAction(action)
			.build();

		return notif;
	}

	public static Notification createInCallNotification(Context context,
	                                                    String title, String msg, int iconID, Bitmap contactIcon,
	                                                    String contactName, PendingIntent intent) {

		Notification notif = new Notification.Builder(context, context.getString(R.string.notification_service_channel_id))
			.setContentTitle(contactName)
			.setContentText(msg)
			.setSmallIcon(iconID)
			.setAutoCancel(false)
			.setContentIntent(intent)
			.setLargeIcon(contactIcon)
			.setCategory(Notification.CATEGORY_CALL)
			.setVisibility(Notification.VISIBILITY_PUBLIC)
			.setPriority(Notification.PRIORITY_HIGH)
			.setWhen(System.currentTimeMillis())
			.setShowWhen(true)
			.setColor(context.getColor(R.color.notification_color_led))
			.build();

		return notif;
	}

	public static Notification createNotification(Context context, String title, String message, int icon, int level, Bitmap largeIcon, PendingIntent intent, boolean isOngoingEvent,int priority) {
		Notification notif;

		if (largeIcon != null) {
			notif = new Notification.Builder(context, context.getString(R.string.notification_service_channel_id))
				.setContentTitle(title)
				.setContentText(message)
				.setSmallIcon(icon, level)
				.setLargeIcon(largeIcon)
				.setContentIntent(intent)
				.setCategory(Notification.CATEGORY_SERVICE)
				.setVisibility(Notification.VISIBILITY_SECRET)
				.setPriority(priority)
				.setWhen(System.currentTimeMillis())
				.setShowWhen(true)
				.setColor(context.getColor(R.color.notification_color_led))
				.build();
		} else {
			notif = new Notification.Builder(context, context.getString(R.string.notification_service_channel_id))
				.setContentTitle(title)
				.setContentText(message)
				.setSmallIcon(icon, level)
				.setContentIntent(intent)
				.setCategory(Notification.CATEGORY_SERVICE)
				.setVisibility(Notification.VISIBILITY_SECRET)
				.setPriority(priority)
				.setWhen(System.currentTimeMillis())
				.setShowWhen(true)
				.setColor(context.getColor(R.color.notification_color_led))
				.build();
		}

		return notif;
	}

	public static void removeGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener keyboardListener) {
		viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener);
	}

	public static Notification createMissedCallNotification(Context context, String title, String text, PendingIntent intent) {
		Notification notif = new Notification.Builder(context, context.getString(R.string.notification_channel_id))
			.setContentTitle(title)
			.setContentText(text)
			.setSmallIcon(R.drawable.call_status_missed)
			.setAutoCancel(true)
			.setContentIntent(intent)
			.setDefaults(Notification.DEFAULT_SOUND
					| Notification.DEFAULT_VIBRATE)
			.setCategory(Notification.CATEGORY_MESSAGE)
			.setVisibility(Notification.VISIBILITY_PRIVATE)
			.setPriority(Notification.PRIORITY_HIGH)
			.setWhen(System.currentTimeMillis())
			.setShowWhen(true)
			.setColor(context.getColor(R.color.notification_color_led))
			.build();

		return notif;
	}

	public static Notification createSimpleNotification(Context context, String title, String text, PendingIntent intent) {
		Notification notif = new Notification.Builder(context, context.getString(R.string.notification_channel_id))
			.setContentTitle(title)
			.setContentText(text)
			.setSmallIcon(R.drawable.linphone_logo)
			.setAutoCancel(true)
			.setContentIntent(intent)
			.setDefaults(Notification.DEFAULT_SOUND
					| Notification.DEFAULT_VIBRATE)
			.setCategory(Notification.CATEGORY_MESSAGE)
			.setVisibility(Notification.VISIBILITY_PRIVATE)
			.setPriority(Notification.PRIORITY_HIGH)
			.setWhen(System.currentTimeMillis())
			.setShowWhen(true)
			.setColorized(true)
			.setColor(context.getColor(R.color.notification_color_led))
			.build();

		return notif;
	}

	public static void startService(Context context, Intent intent) {
		context.startForegroundService(intent);
	}

	public static void setFragmentTransactionReorderingAllowed(FragmentTransaction transaction, boolean allowed) {
		transaction.setReorderingAllowed(allowed);
	}
}
