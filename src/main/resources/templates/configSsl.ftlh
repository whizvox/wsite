<#include "macros.ftlh">
<@htmlBase "Configure SSL" siteName>
<form id="configSsl" name="configSsl" method="post">
  <@sslConfigForm config />
  <input type="submit">
</form>
</@htmlBase>