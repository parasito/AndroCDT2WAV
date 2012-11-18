package es.aurdroid.androcdt2wav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import es.aurdroid.cdt2wav.CDT2WAV;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity implements OnCompletionListener,
		SeekBar.OnSeekBarChangeListener {

	private static final int REQUEST_PICK_FILE = 1;


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
	
	private TextView textViewInfo;
	// Media Player
	private MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;

	private Utilities utils = new Utilities();

	private int seekForwardTime = 5000; // 5000 milliseconds
	private int seekBackwardTime = 5000; // 5000 milliseconds
	
	private ProgressDialog pd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
				// extensions.add(".docx");
				// extensions.add(".txt");
				// extensions.add(".rtf");
				intent.putExtra(
						FilePickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS,
						extensions);

				// Start the activity
				startActivityForResult(intent, REQUEST_PICK_FILE);
			}
		});


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_PICK_FILE:
			if (resultCode == FilePickerActivity.RESULT_OK) {
				//Delete previus file
				this.deleteFile();
				this.deleteFile = true;
				//Set save status
				//this.btnSave.setEnabled(true);
				// A filename (absolute path) was returned. Try to display it.
				final String filename = data
						.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH);
				// File file = new File(filename);
				System.out.println("File name selected: " + filename);
				songTitleLabel.setText(data
						.getStringExtra(FilePickerActivity.EXTRA_FILE_NAME));

				//Progress Dialog
				pd = ProgressDialog.show(this,
						this.getString(R.string.progress_title),
						this.getString(R.string.progress_text), true, false);
				new ConvertTask().execute(filename);
			} else {
				System.out.println("Exiting with result code = " + resultCode);
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
			  
			// Running this thread after 100 milliseconds
			mHandler.postDelayed(this, 100);
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		deleteFile();
		mp.stop();
		mp.release();
	}

}
