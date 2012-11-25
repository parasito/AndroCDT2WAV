package es.aurdroid.androcdt2wav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;


import es.aurdroid.cdt2wav.CDT2WAV;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnCompletionListener,
		SeekBar.OnSeekBarChangeListener {
	
	/**
	 * Log level
	 */
	public final static String LOG = "AndroCDT2WAV";

	private static final int REQUEST_PICK_FILE = 1;
	private static final String PREF_PATH = "es.aurdroid.androcdt2wav.path";
	
	SharedPreferences prefs;
	

	private int[] blocks;
	private String[] ids;
	private byte[] tapesample;

	int divider = 500;
	int h = 0;
	boolean wait = false;

	boolean playing = false;
	int freq = 44100;

	private String playname = "";
	private String savename = "";
	private String lastPath = "";
	private String extension = "";

	boolean fileToPlay = false;
	boolean deleteFile = true;

	// Controles
	private ImageButton btnPlay;
	private ImageButton btnForward;
	private ImageButton btnBackward;
	private ImageButton btnPrevious;
	private ImageButton btnPlaylist;
	private ImageButton btnSave;

	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	private ImageView imageViewAmstrad;
	private ImageView imageViewSinclair;
	
	private TextView textViewInfo;
	// Media Player
	private MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;

	private Utilities utils = new Utilities();

	private int seekForwardTime = 5000; // 5000 milliseconds
	private int seekBackwardTime = 5000; // 5000 milliseconds
	
	private ProgressDialog pd;
	private ConvertTask convertTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	    
		//Preferences
		prefs = this.getSharedPreferences(
			      "es.aurdroid.androcdt2wav", Context.MODE_PRIVATE);
		
		// All player buttons
		btnPlay = (ImageButton) findViewById(R.id.btnPlay);
		btnForward = (ImageButton) findViewById(R.id.btnForward);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
		btnSave = (ImageButton) findViewById(R.id.btnSave);
		//btnSave.setEnabled(false);

		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		
		textViewInfo = (TextView) findViewById(R.id.textViewInfo);
		
		imageViewAmstrad = (ImageView) findViewById(R.id.brandImageView1);
		imageViewSinclair = (ImageView) findViewById(R.id.brandImageView2);
		
		// Mediaplayer
		mp = new MediaPlayer();

		// Listeners
		songProgressBar.setOnSeekBarChangeListener(this); // Important
		mp.setOnCompletionListener(this); // Important

		/**
		 * Play button click event plays a song and changes button to pause
		 * image pauses a song and changes button to play image
		 * */
		btnPlay.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				// There is something to play
				if (fileToPlay == false)
					return;
				// check for already playing
				if (mp.isPlaying()) {
					if (mp != null) {
						mp.pause();
						// Changing button image to play button
						btnPlay.setImageResource(R.drawable.btn_play);
					}
				} else {
					// Resume song
					if (mp != null) {
						mp.start();
						// Changing button image to pause button
						btnPlay.setImageResource(R.drawable.btn_pause);
					}
				}

			}
		});

		/**
		 * Forward button click event Forwards song specified seconds
		 * */
		btnForward.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				// get current song position
				int currentPosition = mp.getCurrentPosition();
				// check if seekForward time is lesser than song duration
				if (currentPosition + seekForwardTime <= mp.getDuration()) {
					// forward song
					mp.seekTo(currentPosition + seekForwardTime);
				} else {
					// forward to end position
					mp.seekTo(mp.getDuration());
				}
			}
		});

		/**
		 * Backward button click event Backward song to specified seconds
		 * */
		btnBackward.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				// get current song position
				int currentPosition = mp.getCurrentPosition();
				// check if seekBackward time is greater than 0 sec
				if (currentPosition - seekBackwardTime >= 0) {
					// forward song
					mp.seekTo(currentPosition - seekBackwardTime);
				} else {
					// backward to starting position
					mp.seekTo(0);
				}

			}
		});
		
		/**
		 * Back button click event Plays it goes to beguining
		 * */
		btnPrevious.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				// backward to starting position
				if (mp == null)
					return;
				mp.seekTo(0);
				//Pause
				// check for already playing
				if (mp.isPlaying()) {
					mp.pause();
					// Changing button image to play button
					btnPlay.setImageResource(R.drawable.btn_play);
				}
				
			}
		});

		/**
		 * Button Click event for Save avoids deleting song 
		 * 
		 * */
		btnSave.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				deleteFile = false;
				//btnSave.setEnabled(false);
			}
		});
		
		/**
		 * Button Click event for Play list click event Launches list activity
		 * which displays list of songs
		 * */
		btnPlaylist.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				Intent intent = new Intent(getApplicationContext(),
						FilePickerActivity.class);

				// Set the initial directory to be the sdcard
				// intent.putExtra(FilePickerActivity.EXTRA_FILE_PATH,
				// Environment.getExternalStorageState());
				// intent.putExtra("remote_folder", "/sdcard");
				// Show hidden files
				// intent.putExtra(FilePickerActivity.EXTRA_SHOW_HIDDEN_FILES,
				// true)S;

				// Only make certain files visible
				ArrayList<String> extensions = new ArrayList<String>();
				// extensions.add(".pdf");
				extensions.add(".cdt");
				extensions.add(".tzx");
				// extensions.add(".txt");
				// extensions.add(".rtf");
				intent.putExtra(
						FilePickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS,
						extensions);
				
				//Path to begin
				intent.putExtra(FilePickerActivity.EXTRA_FILE_PATH,
						readPathFromPrefs());
				// Get intent extras
				//if(getIntent().hasExtra(EXTRA_FILE_PATH)) {
				//	mDirectory = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));

				// Start the activity
				startActivityForResult(intent, REQUEST_PICK_FILE);
			}
		});
		
		// Get the intent that started this activity
		Intent intent = getIntent();

		if (intent != null) {
			// Called from another app
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {

				final Uri data = intent.getData();

				if (data != null) {
					Log.d(LOG, "> Got data   : " + data);
					final String filePath = data.getEncodedPath();
					Log.d(LOG, "> Open file  : " + filePath);
					onFileReceived(filePath);
				} 
			}
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.menu_settings:
	        	AlertDialog ad = new AlertDialog.Builder(this).create();  
	        	ad.setCancelable(false); // This blocks the 'BACK' button  
	        	ad.setMessage(this.getString(R.string.about_message));  
	        	ad.setButton("OK", new DialogInterface.OnClickListener() {  
	        	    public void onClick(DialogInterface dialog, int which) {  
	        	        dialog.dismiss();                      
	        	    }  
	        	});  
	        	ad.show();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_PICK_FILE:
			if (resultCode == FilePickerActivity.RESULT_OK) {
/*				//Delete previous file
				this.deleteFile();
				this.deleteFile = true;
				//Set save status
				//this.btnSave.setEnabled(true);
				// A filename (absolute path) was returned. Try to display it.
				final String filename = data
						.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH);
				//get Extension
				setExtension(filename);
				//Save path
				this.savePathPrefs(filename);
				// File file = new File(filename);
				System.out.println("File name selected: " + filename);
				songTitleLabel.setText(data
						.getStringExtra(FilePickerActivity.EXTRA_FILE_NAME));

				//Progress Dialog
				pd = ProgressDialog.show(this,
						this.getString(R.string.progress_title),
						this.getString(R.string.progress_text), true, false);
				//Thread to convert file
				convertTask = new ConvertTask();
				convertTask.execute(filename);
	*/
				processFile(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH), 
						data.getStringExtra(FilePickerActivity.EXTRA_FILE_NAME));
			} else {
				Log.d(LOG, "> Exiting with result code = " + resultCode);
				return;
			}
			break;

		default:
			// Shouldn't happen
			System.out
					.println("Request code on activity result not recognized");
			finish();
		}
	}
	
	/**
     * Subclase privada que crea un hilo aparte para realizar
     * las acciones que deseemos.
     */
    private class ConvertTask extends AsyncTask <String, String, String>{
		@Override
		protected String doInBackground(String... filename) {
			MainActivity.this.Convert(filename[0]);
			return null;
		}
		
		protected void onPostExecute(String cadena) {
			              pd.dismiss();			         
			              MainActivity.this.feedInfo();
			              MainActivity.this.playSong();
			              MainActivity.this.displayLogo();
			          }

 
    }

	private void Convert(byte[] data) {
		this.tapesample = new byte[10000];

		// high frecuence
		this.freq = 44100;
		this.divider = 400;

		// mid frecuence
		// this.freq = 22050;
		// this.divider = 200;

		// low frecuence
		// this.freq = 11025;
		// this.divider = 100;

		CDT2WAV cdt2wav = new CDT2WAV(data, this.freq, true);
		this.tapesample = cdt2wav.convert();
		// this.scroll.getHorizontalScrollBar().setValue(0);
		// this.progress.setValue(0);
		this.blocks = cdt2wav.blocks;
		this.ids = cdt2wav.ids;
		this.blocks[(this.blocks.length - 1)] = (this.tapesample.length - 44);
		this.ids[(this.ids.length - 1)] = "Eject tape";
		cdt2wav.dispose();
	}

	protected void Convert(String name) {
		System.out.println("Convirtiendo el fichero " + name);
		File cdt = new File(name);
		this.savename = name;
		this.savename = this.savename.replace(".cdt", "");
		this.savename = this.savename.replace(".CDT", "");
		this.savename = this.savename.replace(".tzx", "");
		this.savename = this.savename.replace(".TZX", "");
		this.savename += ".wav";
		// if (this.player != null) {
		// this.player.stop();
		// this.player.close();
		// }
		try {
			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(cdt));
			int length = in.available();
			byte[] data = new byte[length];
			in.read(data);
			in.close();
			Convert(data);
			if ((this.tapesample != null) && (this.tapesample.length > 10)) {
				// this.save.setEnabled(true);
				this.divider = (this.tapesample.length / 50000);
				//feedInfo();
				Save(this.savename);
			} // else {
				// this.save.setEnabled(false);
				// }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void feedInfo() {
		System.out.println("Rellenando info.......");
		this.textViewInfo.setText("");
		for (int i = 0; i < this.blocks.length; i++) {
			int o = i;
			String pre = "";
			if (o < 1000) {
				pre = pre + "0";
			}
			if (o < 100) {
				pre = pre + "0";
			}
			if (o < 10) {
				pre = pre + "0";
			}
			pre = pre + i;
			this.textViewInfo.append(pre + " - " + this.ids[i] + "\r\n");
		}
	}

	/*
	 * Save
	 */
	private void Save(String name) {
		File cdt = new File(name);
		try {
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(cdt));
			out.write(this.tapesample);
			out.close();
			this.playname = name;
			this.fileToPlay = true;
			/*
			 * this.play.setEnabled(true); this.stop.setEnabled(true);
			 * this.rewind.setEnabled(true); prepareToPlay();
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to play a song
	 * 
	 * */
	public void playSong() {
		// Play song
		try {
			mp.reset();
			mp.setDataSource(this.savename);
			// mp.setDataSource(songsList.get(songIndex).get("songPath"));
			mp.prepare();
			mp.start();
			System.out.println("Reproduciendo canción...");
			// Displaying Song title
			// String songTitle = songsList.get(songIndex).get("songTitle");
			// songTitleLabel.setText(songTitle);

			// Changing Button Image to pause image
			btnPlay.setImageResource(R.drawable.btn_pause);

			// set Progress bar values
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);

			// Updating progress bar
			updateProgressBar();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update timer on seekbar
	 * */
	public void updateProgressBar() {
		mHandler.postDelayed(mUpdateTimeTask, 100);
	}

	/**
	 * Background Runnable thread
	 * */
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			try
			{
				long totalDuration = mp.getDuration();
				long currentDuration = mp.getCurrentPosition();
	
				// Displaying Total Duration time
				songTotalDurationLabel.setText("" +	utils.milliSecondsToTimer(totalDuration)); 
				// Displaying time completed playing 
				songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer(currentDuration));
				  
				// Updating progress bar 
				int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
				//Log.d("Progress", ""+progress);
				songProgressBar.setProgress(progress);
				  
				//if (totalDuration < currentDuration)
					// Running this thread after 100 milliseconds
					mHandler.postDelayed(this, 100);
			}
			catch (Exception e)
			{
				System.out.println("Exception: " + e.getMessage());
			}
		}
	};

	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {


	}

	/**
	 * When user starts moving the progress handler
	 * */
	public void onStartTrackingTouch(SeekBar seekBar) {
		// remove message Handler from updating progress bar
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	/**
	 * When user stops moving the progress hanlder
	 * */
	public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
		int totalDuration = mp.getDuration();
		int currentPosition = utils.progressToTimer(seekBar.getProgress(),
				totalDuration);

		// forward or backward to certain seconds
		mp.seekTo(currentPosition);

		// update timer progress again
		updateProgressBar();
	}

	/**
	 * On Song Playing completed if repeat is ON play same song again if shuffle
	 * is ON play random song
	 * */
	public void onCompletion(MediaPlayer arg0) {

	}
	
	/***
	 * Deletes temporal file
	 * @return boolean deleted
	 */
	private boolean deleteFile(){
		if (this.deleteFile == true)
		{
			if (this.savename.compareTo("") != 0){
				System.out.println("Borrando fichero....");
				File file = new File(this.savename);
				return file.delete();
			}
		}
		return false;
		
	}
	
	private void setExtension(String name){
		//TXZ or CDT
		extension = name.substring(name.length()-3, name.length());
	}
	
	private void displayLogo(){
		//TODO hacer esto fuera del hilo
		
		if (extension.compareToIgnoreCase("CDT") == 0){
			imageViewAmstrad.setVisibility(View.VISIBLE);
			imageViewSinclair.setVisibility(View.INVISIBLE);
		}
		else{
			imageViewSinclair.setVisibility(View.VISIBLE);
			imageViewAmstrad.setVisibility(View.INVISIBLE);
		}
	}
	
	
	/*
	 * Reads path from preferences
	 * @return paths
	 */
	protected String readPathFromPrefs(){
		return prefs.getString(PREF_PATH, "/");
	}
	
	/*
	 * Stores path in preferences
	 */
	protected void savePathPrefs(String path){
		//Delete file name
		int index = path.lastIndexOf("/");
		if (index > 0)
			path = path.substring(0, index);
		System.out.println(path);
		prefs.edit().putString(PREF_PATH, path).commit();
	}
	
	/*
	 * A file is received from another app
	 */
	private void onFileReceived(String file) {
		//Parse file Name
		//file = StringEscapeUtils.unescapeHtml3(file);
		try {
			String decode = URLDecoder.decode(file, "UTF-8");
			Log.d(LOG, "> File decoded : " + decode);
			file = decode;
			//file = file.replace("%20"," ");
			Log.d(LOG, "> File parsed : " + file);
			File newFile = new File(file);
			Log.d(LOG, "> File received :" + newFile.getAbsolutePath());
			if (newFile.isFile()) {
				// Set result		
				Log.d(LOG, "> FileReceived OK : " + newFile.getAbsolutePath());
				//Process the file
				processFile(newFile.getAbsolutePath(), newFile.getName());
			} else {
				Log.d(LOG, "> File not valid ");
				Toast.makeText(this, this.getString(R.string.file_not_valid), Toast.LENGTH_SHORT).show();
				setResult(RESULT_CANCELED);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Log.d(LOG, "> Exception: " + e.getMessage());
		}
	}
	
	/*
	 * Process and play file
	 */
	private void processFile (String path, String name){
		//Delete previous file
		this.deleteFile();
		this.deleteFile = true;
		//Set save status
		//this.btnSave.setEnabled(true);
		// A filename (absolute path) was returned. Try to display it.
		//get Extension
		setExtension(path);
		//Save path
		this.savePathPrefs(path);
		Log.d(LOG, ">File name selected: " + path);
		songTitleLabel.setText(name);

		//Progress Dialog
		pd = ProgressDialog.show(this,
				this.getString(R.string.progress_title),
				this.getString(R.string.progress_text), true, false);
		//Thread to convert file
		convertTask = new ConvertTask();
		convertTask.execute(path);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		deleteFile();
		if (mp != null){
			mp.stop();
			mp.release();
			mp = null;
		}
		if (convertTask != null)
			if (convertTask.isCancelled() == false)
				convertTask.cancel(true);
	}

}
