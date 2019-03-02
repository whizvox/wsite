<#include "macros.ftlh">
<@htmlBase "Configure SMTP" siteName>
<form id="configSmtp" name="configSmtp" method="post">
  <@smtpConfigForm config />
  <input type="submit">
</form>
</@htmlBase>