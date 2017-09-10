package com.longstox.stockboard;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class StockLookupAdapter extends
        ArrayAdapter<StockLookupAdapter.StockInfo> {

	private static final String LOG_TAG = StockLookupAdapter.class
	        .getSimpleName();

	class StockInfo {
		public String symbol;
		public String name;
		public String exchange;

		@Override
		public String toString() {
			// text to display in the auto-complete dropdown
			return symbol + " (" + name + ")";
		}
	}

	private final StockLookupFilter filter = new StockLookupFilter();

	public StockLookupAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
	}

	@Override
	public Filter getFilter() {
		return filter;
	}

	private class StockLookupFilter extends Filter {

		// Invoked in a worker thread to filter the data according to the
		// constraint.
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint != null) {
				ArrayList<StockInfo> list = lookupStock(constraint);
				results.values = list;
				results.count = list.size();
			}
			return results;
		}

		private ArrayList<StockInfo> lookupStock(CharSequence constraint) {
			ArrayList<StockInfo> list = new ArrayList<StockInfo>();
			String response = null;
			try {
				URL url = new URL(
				        "http://d.yimg.com/aq/autoc?region=US&lang=en-US&callback=YAHOO.util.ScriptNodeDataSource.callbacks&query="
				                + URLEncoder.encode(constraint.toString(),
				                        "UTF-8"));
				response = WebClient.get(url);
				String prefix = "YAHOO.util.ScriptNodeDataSource.callbacks(";
				String suffix = ");";
				if (!response.startsWith(prefix) || !response.endsWith(suffix))
					throw new Exception("Unexpected response: " + response);
				String json = response.substring(prefix.length(),
				        response.length() - suffix.length());
				JSONObject obj = new JSONObject(json);
				JSONArray array = obj.getJSONObject("ResultSet").getJSONArray(
				        "Result");
				for (int i = 0; i < array.length(); ++i) {
					JSONObject stock = array.getJSONObject(i);
					StockInfo info = new StockInfo();
					info.symbol = stock.getString("symbol");
					info.name = stock.getString("name");
					info.exchange = stock.isNull("exchDisp") ? stock
					        .getString("exch") : stock.getString("exchDisp");
					list.add(info);
				}
			} catch (Exception e) {
				if (response == null)
					Log.e(LOG_TAG, "comm error", e);
				else
					Log.e(LOG_TAG, "Error parsing response response: "
					        + response, e);
			}
			return list;
		}

		// Invoked in the UI thread to publish the filtering results in the user
		// interface.
		@Override
		protected void publishResults(CharSequence constraint,
		        FilterResults results) {
			setNotifyOnChange(false);
			clear();
			if (results.count > 0) {
				addAll((ArrayList<StockInfo>) results.values);
				notifyDataSetChanged();
			} else {
				notifyDataSetInvalidated();
			}

		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			if (resultValue instanceof StockInfo) {
				// text to set in the text view when an item from the dropdown
				// is selected
				return ((StockInfo) resultValue).symbol;
			}
			return null;
		}

	}
}
