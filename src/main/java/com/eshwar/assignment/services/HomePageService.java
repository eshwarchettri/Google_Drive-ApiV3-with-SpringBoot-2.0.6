/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eshwar.assignment.services;

import com.eshwar.assignment.util.Common;
import com.eshwar.assignment.util.FileUploadProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author eshwar chettri
 */
@Service
public class HomePageService {
   public static void downloadReport(HttpServletResponse response, String repFilePath, String repFileName, String contentType) {
        java.io.File f = new java.io.File(repFilePath + repFileName);
        long size = f.length();
        String zippedFileName = "";
        boolean zipped = false;
        response.setHeader("Cache-Control", "private, max-age=15");
        if (zipped) {
            response.setContentType("application/zip");
            f = new java.io.File(repFilePath + zippedFileName);
        } else if (contentType.equals("pdf")) {
            response.setContentType("application/pdf");
        } else if (contentType.equals("zip")) {
            response.setContentType("application/zip");
        } else if (contentType.equals("tsv")) {
            response.setContentType("text/tab-separated-values");
        } else if (contentType.equals("xlsx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (contentType.equals("docs")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else {
            response.setContentType("application/vnd.ms-excel");
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + f.getName());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.out.println("Exception.." + e);
        } catch (Exception e) {
            System.out.println("Exception.." + e);
        }
        try {
            ServletOutputStream resOut = response.getOutputStream();
            IOUtils.copy(fis, resOut);
            resOut.close();

        } catch (IOException e) {
            System.out.println("Exception.." + e);
        } catch (Exception e) {
            System.out.println("Exception.." + e);
        }
    }

     public static String compressAllFiles(String[] filePath, String zipFileName, String appPath) {
        String zipFilePath = null;
        try {
            byte[] buffer = new byte[2048];
            FileOutputStream outputFile = new FileOutputStream(appPath + zipFileName + ".zip");
            try (ZipOutputStream zipFile = new ZipOutputStream(outputFile)) {
                zipFile.setMethod(ZipOutputStream.DEFLATED);
                
                for (String filePath1 : filePath) {
                    try (final FileInputStream inFile = new FileInputStream(filePath1)) {
                        String fileName = filePath1;
                        zipFile.putNextEntry(new ZipEntry(fileName.substring(fileName.lastIndexOf("/") + 1)));
                        int length;
                        while ((length = inFile.read(buffer)) > 0) {
                            zipFile.write(buffer, 0, length);
                        }
                        zipFile.closeEntry();
                    }
                }
            }
            zipFilePath = appPath + zipFileName + ".zip";
        } catch (IOException ex) {
            System.out.println("Exception" + ex);
        }
        return zipFilePath;
    }

    public static List<String> downloadfiles(Drive service, String DIR_FOR_DOWNLOADS) throws IOException {

//         String pageToken=null;
        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(15)
                .setFields("nextPageToken, files(id, name, fileExtension,mimeType)")
                .execute();
        List<String> allDownloadedFiles = new ArrayList();
        String mimeType = "";
        /*If all the files are required then comment above code and uncomment below code*/
//        List<File> allFiles = new ArrayList<>();

//        do {
//            FileList fileList = service.files().list()
//                    .setFields("nextPageToken, files(id, name, fileExtension,mimeType)")//
//                    .setPageToken(pageToken).execute();
//            for (File file : fileList.getFiles()) {
//                allFiles.add(file);
//            }
//            pageToken = result.getNextPageToken();
//        } while (pageToken != null);
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
                String fname = file.getMimeType();
                String ex = fname.substring(fname.lastIndexOf(".") + 1);
                try {
                    Drive.Files f = service.files();
                    HttpResponse httpResponse = null;
                    if (ex.contains("spreadsheet")) {
                        if (file.getMimeType().equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                            file.setMimeType("application/vnd.google-apps.spreadsheet");
                            httpResponse = f
                                    .get(file.getId())
                                    .executeMedia();
                        } else {
                            httpResponse = f
                                    .export(file.getId(),
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    .executeMedia();
                        }
                        if (file.getFileExtension() == null) {
                            file.setName(file.getName() + ".xlsx");
                        }

                    } else if (ex.contains("document")) {
                        if (file.getMimeType().equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                            file.setMimeType("application/vnd.google-apps.document");
                            httpResponse = f
                                    .get(file.getId())
                                    .executeMedia();
                        } else {
                            httpResponse = f
                                    .export(file.getId(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                    .executeMedia();

                        }

                        if (file.getFileExtension() == null) {
                            file.setName(file.getName() + ".docx");
                        }

                    } else if (ex.contains("presentation")) {
                        if (file.getMimeType().equalsIgnoreCase("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
                            file.setMimeType("application/vnd.google-apps.presentation");
                            httpResponse = f
                                    .get(file.getId())
                                    .executeMedia();
                        } else {
                            httpResponse = f
                                    .export(file.getId(),
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                                    .executeMedia();
                        }
                        if (file.getFileExtension() == null) {
                            file.setName(file.getName() + ".pptx");
                        }

                    } else if (ex.contains("pdf")
                            || ex.contains("jpg")
                            || ex.contains("png")
                            || ex.contains("rar")
                            || ex.contains("zip")) {

                        Drive.Files.Get get = f.get(file.getId());
                        httpResponse = get.executeMedia();
                    }
                    if (null != httpResponse) {
                        InputStream instream = httpResponse.getContent();
                        FileOutputStream output = new FileOutputStream(
                                DIR_FOR_DOWNLOADS + file.getName());
                        try {
                            int l;
                            byte[] tmp = new byte[2048];
                            while ((l = instream.read(tmp)) != -1) {
                                output.write(tmp, 0, l);

                            }
                        } finally {
                            output.close();
                            instream.close();
                            allDownloadedFiles.add(DIR_FOR_DOWNLOADS + file.getName());
                            mimeType = file.getMimeType();
                        }
                    }
                } catch (IOException e) {
                    System.err.println(e);

                }
            }
        }
        if (allDownloadedFiles.size() == 1) {
            allDownloadedFiles.add(1, mimeType);
        }
        return allDownloadedFiles;
    } 
    
    public ResponseEntity<?> uploadFile(MultipartFile file,boolean uploadTofolder,String folderId,Drive drive) throws IOException{
    
        java.io.File filePath = Common.convert(file);
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        if(uploadTofolder){
        fileMetadata.setParents(Collections.singletonList(folderId));
        }
        String s = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        if (s.contains("cvs")) {//If it is a CVS file then converting it to spreadsheet in drive 
            fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        }

        FileContent mediaContent = new FileContent(file.getContentType(), filePath);
        Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent).setFields("id");
        MediaHttpUploader uploader = insert.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(true);
        uploader.setProgressListener(new FileUploadProgressListener());
        if (insert.execute().getId() != null) {
            return new ResponseEntity("Successfully uploaded - "
                    + file.getOriginalFilename(), new HttpHeaders(), HttpStatus.CREATED);
        } else {
            return new ResponseEntity("File Upload Failed - "
                    + file.getOriginalFilename(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }
    
    }
}
