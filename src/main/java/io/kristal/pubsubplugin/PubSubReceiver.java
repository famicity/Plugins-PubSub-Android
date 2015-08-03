/**
 *
 * PubSubReceiver
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

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import java.lang.ref.WeakReference;

import org.json.JSONObject;

/**
 * An object allowing a WebView contained in a CobaltFragment
 * to subscribe/unsubscribe for messages sent via a channel and receive them.
 *
 * @author Sébastien Vitard
 */
public class PubSubReceiver {

    private static final String TAG = PubSubReceiver.class.getSimpleName();

    /***********************************************************************************************
     * MEMBERS
     **********************************************************************************************/

    /**
     * The dictionary which keeps track of subscribed channels and their linked callback
     */
    private SimpleArrayMap<String, String> callbackForChannel;
    /**
     * The CobaltFragment containing the WebView to which send messages
     */
    private WeakReference<CobaltFragment> fragmentReference = new WeakReference<>(null);
    /**
     * The listener to notify when the fragment is nil (deallocated or not correctly initialized)
     * or the PubSubReceiver is not subscribed to any channel any more
     */
    private PubSubInterface listener;

    /***********************************************************************************************
     * METHODS
     **********************************************************************************************/

    /***********************************************************************************************
     * Constructors
     **********************************************************************************************/

    /**
     * Creates and return a PubSubReceiver for the specified CobaltFragment registered to no channel.
     * @param fragment the CobaltFragment containing the WebView to which send messages.
     */
    public PubSubReceiver(CobaltFragment fragment) {
        fragmentReference = new WeakReference<CobaltFragment>(fragment);
        callbackForChannel = new SimpleArrayMap<>();
    }

    /**
     * Creates and return a PubSubReceiver for the specified CobaltFragment registered to the specified channel.
     * @param fragment the CobaltFragment containing the WebView to which send messages.
     * @param callback the callback to call to forward messages from the specified channel.
     * @param channel  the channel from which the messages will come from.
     */
    public PubSubReceiver(CobaltFragment fragment, String callback, String channel) {
        fragmentReference = new WeakReference<CobaltFragment>(fragment);
        callbackForChannel = new SimpleArrayMap<>(1);
        callbackForChannel.put(channel, callback);
    }

    /***********************************************************************************************
     * Getters / Setters
     **********************************************************************************************/

    /**
     * Gets the CobaltFragment containing the WebView to which send messages
     * @return the CobaltFragment containing the WebView to which send messages
     */
    public CobaltFragment getFragment() {
        return fragmentReference.get();
    }

    /**
     * Sets the listener to notify when the fragment is nil (deallocated or not correctly initialized)
     * or the PubSubReceiver is not subscribed to any channel any more
     * @param listener the listener
     */
    public void setListener(PubSubInterface listener) {
        this.listener = listener;
    }

    /***********************************************************************************************
     * Helpers
     **********************************************************************************************/

    /**
     * Subscribes to messages sent from the specified channel.
     *
     * @param callback the callback to call to forward messages from the specified channel.
     * @param channel  the channel from which the messages will come from.
     * @implNote overrides the callback if the PubSubReceiver has already subscribed to the specified channel
     */
    public void subscribeToChannel(@NonNull String channel, String callback) {
        callbackForChannel.put(channel, callback);
    }

    /**
     * Unsubscribes from messages sent from the specified channel.
     *
     * @param channel the channel from which the messages come from.
     * @implNote if after the unsubscription, the PubSubReceiver is not subscribed to any channel and delegate is set,
     * its receiverReadyForRemove: method will be called.
     */
    public void unsubscribeFromChannel(@NonNull String channel) {
        callbackForChannel.remove(channel);

        if (callbackForChannel.isEmpty()
            && listener != null) {
            listener.receiverReadyForRemove(this);
        }
    }

    /**
     * If the PubSubReceiver has subscribed to the specified channel, sends the specified message from this channel to the WebView contained in the fragment
     *
     * @param message the message received from the channel.
     * @param channel the channel from which the messages come from.
     * @implNote if fragment is nil at this time, due to deallocation or wrong initialization,
     * and the delegate is set, its receiverReadyForRemove: method will be called.
     */
    public void receiveMessage(JSONObject message, @NonNull String channel) {
        CobaltFragment fragment = fragmentReference.get();
        if (fragment == null) {
            Log.w(TAG, "receiveMessage - fragment is null. "
                        + "It may be caused by its deallocation or the PubSubReceiver was not correctly initialized... "
                        + "Please check if the PubSubReceiver has been initialized with PubSubReceiver(CobaltFragment) or PubSubReceiver(CobaltFragment, String, String) methods.");

            if (listener != null) {
                listener.receiverReadyForRemove(this);
            }

            return;
        }

        String callback = callbackForChannel.get(channel);
        if (callback == null) {
            Log.w(TAG, "receiveMessage - " + fragment.getClass().getSimpleName() + "has not subscribed to " + channel + " channel yet or has already unsubscribed.");
            return;
        }

        fragment.sendCallback(callback, message);
    }
}
