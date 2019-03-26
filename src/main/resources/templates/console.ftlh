<#include "macros.ftlh">
<@htmlBase "Console" siteName>
<h1>Server Log</h1>
<div id="console"></div><br>
<button onclick="scrollToBottom($('console'));return false;">Scroll to bottom</button>
<button id="disconnect">Disconnect</button>
<script>
  var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/control/wsconsole");
  var eConsole = $("console");

  webSocket.onopen = function() {
    insertTextAndAutoScroll(eConsole, "! CONNECTED !<br>")
  };
  webSocket.onmessage = function(msg) {
    // TODO: Maybe make the output prettier to look at? Like with syntax highlighting?
    insertTextAndAutoScroll(eConsole, JSON.parse(msg.data).message + "<br>");
  };
  webSocket.onclose = function() {
    insertTextAndAutoScroll(eConsole, "! DISCONNECTED !<br>");
  };
  $("disconnect").onclick = function() {
    webSocket.close(1000, "Closed by client");
  }
</script>
</@htmlBase>