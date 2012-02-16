/*
 * Copyright (C) 2012 Kazuya (Kaz) Yokoyama <kazuya.yokoyama@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mobisocial.bento.ebento.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import mobisocial.bento.ebento.R;

public class PeopleListFragment extends ListFragment {
    //private static final String TAG = "PeopleListFragment";
    
	private PeopleListItemAdapter mListAdapter = null;
	private ListView mListView = null;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View root = inflater.inflate(R.layout.fragment_people_list, container, false);
        
        // ListView
		mListView = (ListView) root.findViewById(android.R.id.list);
        mListView.setFastScrollEnabled(true);
		
        return root;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
        // Create adapter
        mListAdapter = new PeopleListItemAdapter(
        		getActivity(), 
        		android.R.layout.simple_list_item_1,
        		mListView);
        setListAdapter(mListAdapter);
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

	public void refreshView() {
		Handler handler = new Handler();
		handler.post(new Runnable(){
			public void run(){
				mListAdapter.notifyDataSetChanged();
				mListView.invalidateViews();
			}
		});
    }
}
