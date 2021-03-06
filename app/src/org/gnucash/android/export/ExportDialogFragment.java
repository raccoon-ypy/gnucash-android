/*
 * Copyright (c) 2012-2013 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.export;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.gnucash.android.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog fragment for exporting account information as OFX files.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportDialogFragment extends DialogFragment {
		
	/**
	 * Spinner for selecting destination for the exported file.
	 * The destination could either be SD card, or another application which
	 * accepts files, like Google Drive.
	 */
	Spinner mDestinationSpinner;
	
	/**
	 * Checkbox indicating that all transactions should be exported,
	 * regardless of whether they have been exported previously or not
	 */
	CheckBox mExportAllCheckBox;
	
	/**
	 * Checkbox for deleting all transactions after exporting them
	 */
	CheckBox mDeleteAllCheckBox;
	
	/**
	 * Save button for saving the exported files
	 */
	Button mSaveButton;
	
	/**
	 * Cancels the export dialog
	 */
	Button mCancelButton;
	
	/**
	 * File path for saving the OFX files
	 */
	String mFilePath;
	
	/**
	 * Tag for logging
	 */
	private static final String TAG = "ExportDialogFragment";

    ;

    private ExportFormat mExportFormat = ExportFormat.QIF;

	/**
	 * Click listener for positive button in the dialog.
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
	protected class ExportClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
            ExportParams exportParameters = new ExportParams(mExportFormat);
            exportParameters.setExportAllTransactions(mExportAllCheckBox.isChecked());
            exportParameters.setTargetFilepath(mFilePath);
            int position = mDestinationSpinner.getSelectedItemPosition();
            exportParameters.setExportTarget(position == 0 ? ExportParams.ExportTarget.SHARING : ExportParams.ExportTarget.SD_CARD);
            exportParameters.setDeleteTransactionsAfterExport(mDeleteAllCheckBox.isChecked());

            dismiss();

            Log.i(TAG, "Commencing async export of transactions");
            new ExporterTask(getActivity()).execute(exportParameters);
		}
		
	}

    public void onRadioButtonClicked(View view){
        switch (view.getId()){
            case R.id.radio_ofx_format:
                mExportFormat = ExportFormat.OFX;
                break;
            case R.id.radio_qif_format:
                mExportFormat = ExportFormat.QIF;
        }
        mFilePath = getActivity().getExternalFilesDir(null) + "/" + buildExportFilename(mExportFormat);
        return;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_export_ofx, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
        bindViews();
		mFilePath = getActivity().getExternalFilesDir(null) + "/" + buildExportFilename(mExportFormat);
		getDialog().setTitle(R.string.title_export_dialog);
	}

	/**
	 * Collects references to the UI elements and binds click listeners
	 */
	private void bindViews(){		
		View v = getView();
		mDestinationSpinner = (Spinner) v.findViewById(R.id.spinner_export_destination);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
		        R.array.export_destinations, android.R.layout.simple_spinner_item);		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mDestinationSpinner.setAdapter(adapter);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mExportAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_export_all);
		mExportAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_export_all_transactions), false));
		
		mDeleteAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_post_export_delete);
		mDeleteAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_delete_transactions_after_export), false));
		
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mSaveButton.setText(R.string.btn_export);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				dismiss();
			}
		});
		
		mSaveButton.setOnClickListener(new ExportClickListener());

        String defaultExportFormat = sharedPrefs.getString(getString(R.string.key_default_export_format), ExportFormat.QIF.name());
        mExportFormat = ExportFormat.valueOf(defaultExportFormat);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRadioButtonClicked(view);
            }
        };

        RadioButton ofxRadioButton = (RadioButton) v.findViewById(R.id.radio_ofx_format);
        ofxRadioButton.setChecked(defaultExportFormat.equalsIgnoreCase(ExportFormat.OFX.name()));
        ofxRadioButton.setOnClickListener(clickListener);

        RadioButton qifRadioButton = (RadioButton) v.findViewById(R.id.radio_qif_format);
        qifRadioButton.setChecked(defaultExportFormat.equalsIgnoreCase(ExportFormat.QIF.name()));
        qifRadioButton.setOnClickListener(clickListener);
	}


	/**
	 * Callback for when the activity chooser dialog is completed
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//TODO: fix the exception which is thrown on return
		if (resultCode == Activity.RESULT_OK){
			//uploading or emailing has finished. clean up now.
			File file = new File(mFilePath);
			file.delete();
		}
	}
	


	/**
	 * Builds a file name based on the current time stamp for the exported file
	 * @return String containing the file name
	 */
	public static String buildExportFilename(ExportFormat format){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
		String filename = formatter.format(
				new Date(System.currentTimeMillis())) 
				+ "_gnucash_all";
        switch (format) {
            case QIF:
                filename += ".qif";
                break;
            case OFX:
                filename += ".ofx";
                break;
        }
		return filename;
	}
}

