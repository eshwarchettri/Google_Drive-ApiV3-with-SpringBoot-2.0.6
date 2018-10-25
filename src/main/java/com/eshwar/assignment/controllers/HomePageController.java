/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloudelements.assignment.controllers;

import com.cloudelements.assignment.services.HomePageService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.google.api.services.drive.model.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Eshwar chettri
 */
@Controller
public class HomePageController {

    @Value("${google.secret.key.path}")
    private Resource gdSecretKey;

    @Value("${google.oauth.callback.url}")
    private String REDIRECT_URI;

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFolder;

    @Value("${files.path}")
    private String DIR_FOR_DOWNLOADS;
    
    @Value("${unique.identifier}")
    private String USER_IDENTIFIER_KEY;

    @Autowired
    HomePageService homePageService;

    public static final long MAX_FILE_SIZE = 2097152;

    private static final String APPLICATION_NAME = "Assignment Project";
    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();//Httptransport is used by the google api in order to make rest api calls
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();//JsonFactory  is used to serialize and deserialize the responses
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);//Drive Scope 
//    private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER12";//Used as a identifier for user
//    private static final String DIR_FOR_DOWNLOADS = "D:\\download\\";

    GoogleAuthorizationCodeFlow flow;//It should be initialized only once when application is started

    @PostConstruct
    public void init() throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretKey.getInputStream()));
        flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile())).build();
    }

    @GetMapping("/")
    public String showHomePage() throws Exception {
        boolean isUserAuthenticated = false;
        Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
        if (credential != null) {
            boolean isValidToken = credential.refreshToken();
            if (isValidToken) {
                isUserAuthenticated = true;
            }
        }

        return isUserAuthenticated ? "dashboard.html" : "index.html";

    }

    @GetMapping("/googlesignin")
    public void doGoogleSignIn(HttpServletResponse response) throws IOException {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectUrl = url.setRedirectUri(REDIRECT_URI).setAccessType("offline").setApprovalPrompt("force").build();
        response.sendRedirect(redirectUrl);

    }

    @GetMapping("/oauth")
    public String saveTokens(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        if (code != null) {
            saveTokens(code);
            return "dashboard.html";
        }
        return "index.html";
    }

    private void saveTokens(String code) throws IOException {
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);
    }

    @GetMapping("/downloadfile")
    public String downloadFile(HttpServletResponse response, Model model) throws Exception {
        homePageService = new HomePageService();
        Drive service = new Drive(HTTP_TRANSPORT, JSON_FACTORY, flow.loadCredential(USER_IDENTIFIER_KEY));

        List<String> allDownloadedFiles = HomePageService.downloadfiles(service, DIR_FOR_DOWNLOADS);

        String repName = "";
        String zipfileName = "allfiles";
        if (allDownloadedFiles.size() > 1) {
            HomePageService.compressAllFiles((String[]) allDownloadedFiles.toArray(new String[allDownloadedFiles.size()]), zipfileName, DIR_FOR_DOWNLOADS);
            repName = URLEncoder.encode(zipfileName, "UTF-8") + ".zip";
            HomePageService.downloadReport(response, DIR_FOR_DOWNLOADS, repName, "zip");
        } else {
            HomePageService.downloadReport(response, DIR_FOR_DOWNLOADS, allDownloadedFiles.get(0).substring(allDownloadedFiles.get(0).lastIndexOf("/")+1), allDownloadedFiles.get(1));
        }
        model.addAttribute("message", "Files download Completed");
        return "dashboard.html";
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        homePageService = new HomePageService();
        Drive drive = new Drive(HTTP_TRANSPORT, JSON_FACTORY, flow.loadCredential(USER_IDENTIFIER_KEY));
        return homePageService.uploadFile(file, false, "",drive);
    }

    @PostMapping("/uploadFileToFolder")
    public ResponseEntity<?> uploadFileToFolder(@RequestParam("filename") MultipartFile file, @RequestParam("folderId") String folderId) throws IOException {
        homePageService = new HomePageService();
        Drive drive = new Drive(HTTP_TRANSPORT, JSON_FACTORY, flow.loadCredential(USER_IDENTIFIER_KEY));
        return homePageService.uploadFile(file, true, folderId,drive);
    }

    @PostMapping("/createFolder")
    public ResponseEntity<List<String>> createFolder(@RequestParam("folderName") String folderName) throws IOException {
        Drive drive = new Drive(HTTP_TRANSPORT, JSON_FACTORY, flow.loadCredential(USER_IDENTIFIER_KEY));
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = drive.files().create(fileMetadata)
                .setFields("id")
                .execute();
        List<String> fileData = new ArrayList<>();
        if (!file.isEmpty()) {
            fileData.add(0, file.getId());
            fileData.add(1, folderName);
            return new ResponseEntity(fileData, new HttpHeaders(), HttpStatus.CREATED);
        }
        return new ResponseEntity(fileData, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}
