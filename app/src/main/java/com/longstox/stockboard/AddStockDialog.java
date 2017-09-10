package com.longstox.stockboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.ViewGroup.LayoutParams;
import android.widget.AutoCompleteTextView;

public class AddStockDialog extends DialogFragment {

	public interface Listener {
		void onOK(String stockSymbol);
	}

	private AutoCompleteTextView symbolText;
	public Listener listener;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		symbolText = new AutoCompleteTextView(getActivity());
		symbolText.setAdapter(new StockLookupAdapter(getActivity()));
		symbolText.setThreshold(1);
		symbolText.setHint(R.string.add_stock_hint);
		LayoutParams layout = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		symbolText.setLayoutParams(layout);
		symbolText.setPadding(4, 4, 4, 4);
		symbolText.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

		builder.setView(symbolText)
				.setTitle(R.string.add_stock_title)
				// Add action buttons
				.setPositiveButton(R.string.add,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								String stockSymbol = symbolText.getText()
										.toString();
								if (listener != null) {
									listener.onOK(stockSymbol);
								}
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// AddStockDialog.this.getDialog().cancel();
							}
						});
		return builder.create();
	}

}