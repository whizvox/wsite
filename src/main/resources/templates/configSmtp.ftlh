<#include "macros.ftlh">
<@htmlBase "Configure SMTP" siteName>
<form id="configSmtp" method="post" action="/control/config">
  <@smtpConfigFormBase config />
  <input type="submit">
</form>
</@htmlBase>