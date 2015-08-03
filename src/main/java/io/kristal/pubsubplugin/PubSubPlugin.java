/**
 *
 * PubSubPlugin
 * PubSub
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Kristal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package io.kristal.pubsubplugin;

import fr.cobaltians.cobalt.fragments.CobaltFragment;
import fr.cobaltians.cobalt.plugin.CobaltAbstractPlugin;
import fr.cobaltians.cobalt.plugin.CobaltPluginWebContainer;

import android.util.Log;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A plugin which allow WebViews contained in CobaltFragments to broadcast messages between them into channels.
 * Handles subscribe/unsubscribe to channel events and publish message event.
 * Broadcasts messages to WebViews which have subscribed to the channel where they are from.
 * @author Sébastien Vitard
 */
public final class PubSubPlugin extends CobaltAbstractPlugin implements PubSubInterface {

	private static final String TAG = PubSubReceiver.class.getSimpleName();

	/**********************************************************************************************
	 * MEMBERS
	 **********************************************************************************************/

	protected static PubSubPlugin sInstance;

	/**
	 * The array which keeps track of PubSubReceivers
	 */
	private ArrayList<PubSubReceiver> receivers = new ArrayList<>();

	/**********************************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************************/

	public static CobaltAbstractPlugin getInstance(CobaltPluginWebContainer webContainer) {
		if (sInstance == null) sInstance = new PubSubPlugin();
		sInstance.addWebContainer(webContainer);
		return sInstance;
	}

    /**********************************************************************************************
     * COBALT METHODS
     **********************************************************************************************/

	@Override
	public void onMessage(CobaltPluginWebContainer webContainer, JSONObject message) {
		try {
			String action = message.getString("action");
			JSONObject data = message.getJSONObject("data");
			String channel = data.getString("channel");
			JSONObject innerMessage = data.optJSONObject("message");
			String callback = data.optString("callback", null);

			switch(action) {
				case "publish":
					publishMessage(innerMessage, channel);
					break;
				case "subscribe":
					subscribeFragmentToChannel(webContainer.getFragment(), channel, callback);
					break;
				case "unsubscribe":
					unsubscribeFragmentFromChannel(webContainer.getFragment(), channel);
					break;
				default:
					break;
			}
		}
		catch(JSONException exception) {
			Log.e(TAG, "onMessage - Some fields may be missing or not of expected type: string action, object data or string data.channel");
		}
	}

	/**********************************************************************************************
	 * HELPERS
	 **********************************************************************************************/

	/**
	 * Broadcasts the specified message to PubSubReceivers which have subscribed to the specified channel.
	 * @param message the message to broadcast to PubSubReceivers via the channel.
	 * @param channel the channel to which broadcast the message.
	 */
	private void publishMessage(JSONObject message, String channel) {
		for (PubSubReceiver receiver : receivers) {
			receiver.receiveMessage(message, channel);
		}
	}

	/**
	 * Subscribes the specified fragment to messages sent via the specified channel.
	 * @implNote if no PubSubReceiver was created for the specified fragment, creates it.
	 * @param fragment the CobaltFragment the PubSubReceiver will have to use to send messages.
	 * @param channel the channel the PubSubReceiver subscribes.
	 * @param callback the callback the PubSubReceiver will have to call to send messages
	 */
	private void subscribeFragmentToChannel(CobaltFragment fragment, String channel, String callback) {
		PubSubReceiver subscribingReceiver = null;

		for (PubSubReceiver receiver : receivers) {
			if (fragment.equals(receiver.getFragment())) {
				subscribingReceiver = receiver;
				break;
			}
		}

		if (subscribingReceiver != null) {
			subscribingReceiver.subscribeToChannel(channel, callback);
		}
		else {
			subscribingReceiver = new PubSubReceiver(fragment, callback, channel);
			subscribingReceiver.setListener(this);
			receivers.add(subscribingReceiver);
		}
	}

	/**
	 * Unsubscribes the specified fragment from messages sent via the specified channel.
	 * @param fragment the fragment to unsubscribes from the channel.
	 * @param channel the channel from which the messages come from.
	 */

	private void unsubscribeFragmentFromChannel(CobaltFragment fragment, String channel) {
		PubSubReceiver unsubscribingReceiver = null;

		for (PubSubReceiver receiver : receivers) {
			if (fragment.equals(receiver.getFragment())) {
				unsubscribingReceiver = receiver;
				break;
			}
		}

		if (unsubscribingReceiver != null) {
			unsubscribingReceiver.unsubscribeFromChannel(channel);
		}
	}

	/**********************************************************************************************
	 * PUBSUB INTERFACE
	 **********************************************************************************************/

	@Override
	public void receiverReadyForRemove(PubSubReceiver receiver) {
		receivers.remove(receiver);
	}
}
