<#include "macros.ftlh">
<@htmlBase "Configure SSL" siteName>
<form id="configSsl" method="post" action="/control/config" enctype="multipart/form-data">
  <@sslConfigFormBase config />
  <input type="submit">
</form>
</@htmlBase>