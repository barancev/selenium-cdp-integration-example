package org.example;

import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.types.network.Response;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
import com.github.kklisura.cdt.services.types.ChromeTab;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SeleniumCdpIntegrationTest {

  RemoteWebDriver driver;
  ChromeDevToolsService cdpService;

  @BeforeEach
  void init() {
    driver = new ChromeDriver();

    Capabilities caps = driver.getCapabilities();
    String debuggerAddress = (String) ((Map<String, Object>) caps.getCapability("goog:chromeOptions")).get("debuggerAddress");
    int debuggerPort = Integer.parseInt(debuggerAddress.split(":")[1]);

    ChromeService chromeService = new ChromeServiceImpl(debuggerPort);
    ChromeTab pageTab = chromeService.getTabs().stream().filter(tab -> tab.getType().equals("page")).findFirst().get();
    cdpService = chromeService.createDevToolsService(pageTab);
  }

  static class ResponseInfo {
    final String url;
    final int status;

    ResponseInfo(String url, int status) {
      this.url = url;
      this.status = status;
    }

    public String toString() {
      return String.format("%s -> %s", url, status);
    }
  }

  @Test
  void canHandleTraffic() {
    List<ResponseInfo> responses = new ArrayList<>();
    Network network = cdpService.getNetwork();
    network.onResponseReceived(event -> {
      Response res = event.getResponse();
      responses.add(new ResponseInfo(res.getUrl(), res.getStatus()));
    });
    network.enable();

    driver.get("http://stahlburg.by/");
    responses.stream().filter(res -> res.status != 200).forEach(System.out::println);
  }

  @AfterEach
  void fin() {
    if (driver != null) {
      driver.quit();
      driver = null;
      cdpService = null;
    }
  }
}
