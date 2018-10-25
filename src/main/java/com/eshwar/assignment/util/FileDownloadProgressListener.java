/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eshwar.assignment.util;

/**
 *
 * @author Eesha chettri
 */
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

public class FileDownloadProgressListener implements MediaHttpDownloaderProgressListener {

	  @Override
	  public void progressChanged(MediaHttpDownloader downloader) {
	    switch (downloader.getDownloadState()) {
	      case MEDIA_IN_PROGRESS:
	        View.downloadStatus("Download is in progress: " + downloader.getProgress());
	        break;
	      case MEDIA_COMPLETE:
	        View.downloadStatus("Download is Complete!");
	        break;
	    }
	  }
	}

