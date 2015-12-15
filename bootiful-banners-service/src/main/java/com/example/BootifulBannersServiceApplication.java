package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.Data;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


/**
 * I have made so many poor life decisions..
 *
 * @author Josh Long
 */
@EnableConfigurationProperties
@EnableSwagger2
@SpringBootApplication
public class BootifulBannersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootifulBannersServiceApplication.class, args);
    }

    @Bean
    public Docket bootifulBannersApi() {
      return new Docket(DocumentationType.SWAGGER_2)
              .select()
              .paths(PathSelectors.regex("/banner"))
              .build();
    }
}

//curl -F "image=@/Users/jlong/Desktop/doge.jpg" -H "Content-Type: multipart/form-data" http://bootiful-banners.cfapps.io/banners
@RestController
class BannerGeneratorRestController {

    public static final String[] MEDIA_TYPES = {
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_GIF_VALUE};

    @Autowired
    private BannerProperties properties;

    @RequestMapping(
            value = "/banner",
            method = RequestMethod.POST,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    ResponseEntity<String> banner(@RequestParam("image") MultipartFile multipartFile,
            @RequestParam(required = false) Integer maxWidth,
            @RequestParam(required = false) Double aspectRatio,
            @RequestParam(required = false) Boolean invert) throws Exception {
        File image = null;
        try {
            image = this.imageFileFrom(multipartFile);
            ImageBanner imageBanner = new ImageBanner(image);

            if(maxWidth == null) {
                maxWidth = this.properties.getMaxWidth();
            }
            if(aspectRatio == null) {
                aspectRatio = this.properties.getAspectRatio();
            }
            if(invert == null) {
                invert = this.properties.isInvert();
            }

            String banner = imageBanner.printBanner(maxWidth, aspectRatio, invert);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(banner);
        } finally {
            if (image != null) {
                if (image.exists())
                    Assert.isTrue(image.delete(), String.format("couldn't delete temporary file %s",
                            image.getPath()));
            }
        }
    }

    private File imageFileFrom(MultipartFile file) throws Exception {
        Assert.notNull(file);
        Assert.isTrue(Arrays.asList(MEDIA_TYPES).contains(file.getContentType().toLowerCase()));
        File tmp = File.createTempFile("banner-tmp-",
                "." + file.getContentType().split("/")[1]);
        try (InputStream i = new BufferedInputStream(file.getInputStream());
             OutputStream o = new BufferedOutputStream(new FileOutputStream(tmp))) {
            FileCopyUtils.copy(i, o);
            return tmp;
        }
    }
}

@Data
@Component
@ConfigurationProperties(prefix = "banner")
class BannerProperties {

    private int maxWidth = 72;

    private double aspectRatio = 0.5;

    private boolean invert;

}
