<#include "macros.ftlh">
<@htmlBase "Configure site" siteName>
<form id="configSite" method="post" action="/control/config">
  <@siteConfigFormBase config />
  <input type="submit">
</form>
</@htmlBase>