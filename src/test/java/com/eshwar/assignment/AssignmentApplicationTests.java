package com.eshwar.assignment;

import com.eshwar.assignment.controllers.HomePageController;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ResourceUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AssignmentApplication.class)
@WebAppConfiguration
@TestPropertySource(locations="classpath:application-test.properties")
@ActiveProfiles("test")
public class AssignmentApplicationTests {

    private MockMvc restLogsMockMvc;
    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private HomePageController homePageController;

    @Value("${files.path}")
    private String DIR_FOR_DOWNLOADS;

    @Before
    public void setup() {
        this.restLogsMockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void downloadFile() throws Exception {
        restLogsMockMvc.perform(get("/downloadfile")).andExpect(status().isOk());
    }

    @Test
    public void uploadFile() throws Exception {
        System.out.println(DIR_FOR_DOWNLOADS);
        File file1 = ResourceUtils.getFile(this.getClass().getResource("/download.png"));
        InputStream uploadStream = new FileInputStream(file1);
        MockMultipartFile file = new MockMultipartFile("file", "download.png", "image/png", uploadStream);
        restLogsMockMvc.perform(MockMvcRequestBuilders.multipart("/uploadFile").file(file)).andExpect(status().is(201));
    }

    @Test
    public void createFolder() throws Exception {
        String folderName = "IntegartionTestFolder";
        restLogsMockMvc.perform(MockMvcRequestBuilders.post("/createFolder").param("folderName", folderName)).andExpect(status().is(201));
    }

}
