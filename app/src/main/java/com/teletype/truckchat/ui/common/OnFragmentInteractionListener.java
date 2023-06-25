package com.teletype.truckchat.ui.common;

import com.teletype.truckchat.ui.conversations.ConversationsAdapter;
import com.teletype.truckchat.ui.road.RoadClosuresAdapter;

public interface OnFragmentInteractionListener {

    void onFragmentInteractionItemClick(ConversationsAdapter.Conversation conversation, boolean ignoreAutoHide);

    void onFragmentInteractionItemLongClick(ConversationsAdapter.Conversation conversation);

    void onFragmentInteractionItemClick(RoadClosuresAdapter.RoadClosure roadClosure);

    void onEmptyData();
}
