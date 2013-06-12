package org.linphone.test;

import junit.framework.Assert;

import org.linphone.LinphoneActivity;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatMessage.State;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.mediastream.Log;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

public class Chat extends SampleTest {

	@SmallTest
	@MediumTest
	@LargeTest
	public void testAInitLinphoneCore() {		
		LinphoneTestManager.createAndStart(aContext, iContext);
		
		solo.sleep(2000);
		Assert.assertEquals(RegistrationState.RegistrationOk, LinphoneTestManager.getLc().getProxyConfigList()[0].getState());
	}
	
	@LargeTest
	public void testBEmptyChatHistory() {
		goToChat();
		
		Assert.assertTrue(solo.searchText(aContext.getString(org.linphone.R.string.no_chat_history)));
	}
	
	@SmallTest
	@MediumTest
	@LargeTest
	public void testCSendTextMessage() {
		goToChat();
		
		solo.enterText(0, "sip:" + iContext.getString(R.string.account_test_calls_login) + "@" + iContext.getString(R.string.account_test_calls_domain));
		solo.clickOnView(solo.getView(org.linphone.R.id.newDiscussion));
		
		solo.enterText(0, iContext.getString(R.string.chat_test_text_sent));
		solo.clickOnView(solo.getView(org.linphone.R.id.sendMessage));
		
		solo.sleep(1000);
		Assert.assertTrue(solo.searchText(iContext.getString(R.string.chat_test_text_sent)));
		Assert.assertEquals(iContext.getString(R.string.chat_test_text_sent), LinphoneTestManager.getInstance().lastMessageReceived);
	}
	
	@LargeTest
	public void testDNotEmptyChatHistory() {
		goToChat();
		
		Assert.assertFalse(solo.searchText(aContext.getString(org.linphone.R.string.no_chat_history)));
		Assert.assertTrue(solo.searchText(iContext.getString(org.linphone.test.R.string.account_test_calls_login)));
	}
	
	@SmallTest
	@MediumTest
	@LargeTest
	public void testEReceiveTextMessage() {
		goToChat();
		solo.clickOnText(iContext.getString(org.linphone.test.R.string.account_test_calls_login));
		
		LinphoneChatRoom chatRoom = LinphoneTestManager.getLc().createChatRoom("sip:" + iContext.getString(R.string.account_linphone_login) + "@" + iContext.getString(R.string.account_linphone_domain));
		LinphoneChatMessage msg = chatRoom.createLinphoneChatMessage(iContext.getString(R.string.chat_test_text_received));
		chatRoom.sendMessage(msg, new LinphoneChatMessage.StateListener() {
			@Override
			public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg,
					State state) {
				Log.e("Chat message state = " + state.toString());
			}
		});

		solo.sleep(1000);
		Assert.assertTrue(solo.searchText(iContext.getString(R.string.chat_test_text_received)));
	}
	
	@SmallTest
	@MediumTest
	@LargeTest
	public void testZShutDownLinphoneCore() {
		LinphoneTestManager.destroy();
	}
	
	private void goToChat() {
		solo.waitForActivity("LinphoneActivity", 2000);
		solo.assertCurrentActivity("Expected Linphone Activity", LinphoneActivity.class);
		
		solo.clickOnView(solo.getView(org.linphone.R.id.chat));
	}
	
}
