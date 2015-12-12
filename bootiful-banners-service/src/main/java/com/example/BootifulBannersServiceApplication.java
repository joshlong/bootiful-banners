package com.example;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

import java.io.*;
import java.util.Arrays;


/**
 * I have made so many poor life decisions..
 *
 * @author Josh Long
 */
@EnableConfigurationProperties
@SpringBootApplication
public class BootifulBannersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootifulBannersServiceApplication.class, args);
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
            @RequestParam(defaultValue = "false") boolean ansiOutput) throws Exception {
        File image = null;
        try {
            image = this.imageFileFrom(multipartFile);
            ImageBanner imageBanner = new ImageBanner(image);

            int maxWidth = this.properties.getMaxWidth();
            double aspectRatio = this.properties.getAspectRatio();
            boolean invert = this.properties.isInvert();

            String banner = imageBanner.printBanner(maxWidth, aspectRatio, invert);
            if(ansiOutput == true) {
                banner = banner
                    .replace("${AnsiColor.DEFAULT}", "\u001B[39m")
                    .replace("${AnsiColor.BLACK}", "\u001B[30m")
                    .replace("${AnsiColor.RED}", "\u001B[31m")
                    .replace("${AnsiColor.GREEN}", "\u001B[32m")
                    .replace("${AnsiColor.YELLOW}", "\u001B[33m")
                    .replace("${AnsiColor.BLUE}", "\u001B[34m")
                    .replace("${AnsiColor.MAGENTA}", "\u001B[35m")
                    .replace("${AnsiColor.CYAN}", "\u001B[36m")
                    .replace("${AnsiColor.WHITE}", "\u001B[37m")
                    .replace("${AnsiColor.BRIGHT_BLACK}", "\u001B[90m")
                    .replace("${AnsiColor.BRIGHT_RED}", "\u001B[91m")
                    .replace("${AnsiColor.BRIGHT_GREEN}", "\u001B[92m")
                    .replace("${AnsiColor.BRIGHT_YELLOW}", "\u001B[93m")
                    .replace("${AnsiColor.BRIGHT_BLUE}", "\u001B[94m")
                    .replace("${AnsiColor.BRIGHT_MAGENTA}", "\u001B[95m")
                    .replace("${AnsiColor.BRIGHT_CYAN}", "\u001B[96m")
                    .replace("${AnsiColor.BRIGHT_WHITE}", "\u001B[97m")
                    .replace("${AnsiBackground.BLACK}", "\u001B[40m")
                    .replace("${AnsiBackground.DEFAULT}", "\u001B[49m");
            }

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
