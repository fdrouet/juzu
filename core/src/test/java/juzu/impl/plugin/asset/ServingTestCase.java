/*
 * Copyright 2013 eXo Platform SAS
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

package juzu.impl.plugin.asset;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import juzu.test.AbstractWebTestCase;
import juzu.test.UserAgent;
import juzu.test.protocol.servlet.JuzuServlet;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ServingTestCase extends AbstractWebTestCase {

  @Deployment(testable = false)
  public static WebArchive createDeployment() {
    return createServletDeployment(true, "plugin.asset.serving");
  }

  @Drone
  WebDriver driver;

  @Test
  public void testSatisfied() throws Exception {

    URL url = applicationURL();
    driver.get(url.toString());


    System.out.println("applicationURL() = " + url);
    System.out.println("driver.getPageSource() = " + driver.getPageSource());

    WebElement script = driver.findElement(By.tagName("script"));
    String src  = script.getAttribute("src");
    url = new URL(url, src);
    url = url.toURI().resolve("./the_resource.txt").toURL();

    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.connect();
    assertEquals(200, conn.getResponseCode());




//    UserAgent ua = assertInitialPage();
//    HtmlPage page = ua.getHomePage();
//
//    // Script
//    HtmlAnchor trigger = (HtmlAnchor)page.getElementById("trigger");
//    trigger.click();
//    List<String> alerts = ua.getAlerts(page);
//    assertEquals(Arrays.asList("OK MEN"), alerts);
//
//    // CSS
//    List<HtmlElement> links = page.getElementsByTagName("link");
//    assertEquals(2, links.size());
//    HtmlLink link1 = (HtmlLink)links.get(0);
//    assertEquals("stylesheet", link1.getRelAttribute());
//    assertEquals("/juzu/main.css", link1.getHrefAttribute());
//    assertEquals("text/css", link1.getTypeAttribute());
//    HtmlLink link2 = (HtmlLink)links.get(1);
//    assertEquals("stylesheet", link2.getRelAttribute());
//    assertEquals("/juzu/main.less", link2.getHrefAttribute());
//    assertEquals("text/less", link2.getTypeAttribute());
  }
}
