package net.osmand.plus.wikivoyage.search;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import net.osmand.ResultMatcher;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.WikivoyageBaseDialogFragment;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.List;

public class WikivoyageSearchDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = "WikivoyageSearchDialogFragment";

	private WikivoyageSearchHelper searchHelper;
	private String searchQuery = "";

	private boolean paused;
	private boolean cancelled;

	private SearchRecyclerViewAdapter adapter;

	private EditText searchEt;
	private ImageButton clearIb;
	private ProgressBar progressBar;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		searchHelper = new WikivoyageSearchHelper(getMyApplication());

		final View mainView = inflate(R.layout.fragment_wikivoyage_search_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		setupToolbar(toolbar);

		searchEt = (EditText) toolbar.findViewById(R.id.searchEditText);
		searchEt.setHint(R.string.shared_string_search);
		searchEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				String newQuery = s.toString();
				if (!searchQuery.equalsIgnoreCase(newQuery)) {
					searchQuery = newQuery;
					if (searchQuery.isEmpty()) {
						cancelSearch();
						adapter.setItems(null);
					} else {
						runSearch();
					}
				}
			}
		});

		progressBar = (ProgressBar) toolbar.findViewById(R.id.searchProgressBar);

		clearIb = (ImageButton) toolbar.findViewById(R.id.clearButton);
		clearIb.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		clearIb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchEt.setText("");
			}
		});

		adapter = new SearchRecyclerViewAdapter(getMyApplication());
		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);
		adapter.setOnItemClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = rv.getChildAdapterPosition(v);
				if (pos != RecyclerView.NO_POSITION) {
					Object item = adapter.getItem(pos);
					if (item instanceof WikivoyageSearchResult) {
						WikivoyageArticleDialogFragment.showInstance(getFragmentManager(),
								(WikivoyageSearchResult) item);
					}
				}
			}
		});

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		searchEt.requestFocus();
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
	}

	private void cancelSearch() {
		cancelled = true;
		if (!paused) {
			switchProgressBarVisibility(false);
		}
	}

	private void runSearch() {
		switchProgressBarVisibility(true);
		cancelled = false;
		searchHelper.search(searchQuery, new ResultMatcher<List<WikivoyageSearchResult>>() {
			@Override
			public boolean publish(final List<WikivoyageSearchResult> results) {
				getMyApplication().runInUIThread(new Runnable() {
					public void run() {
						if (!isCancelled()) {
							adapter.setItems(results);
							switchProgressBarVisibility(false);
						}
					}
				});
				return true;
			}

			@Override
			public boolean isCancelled() {
				return paused || cancelled;
			}
		});
	}

	private void switchProgressBarVisibility(boolean show) {
		progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
		clearIb.setVisibility(show ? View.GONE : View.VISIBLE);
	}

	public static boolean showInstance(FragmentManager fm) {
		try {
			WikivoyageSearchDialogFragment fragment = new WikivoyageSearchDialogFragment();
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
