<#include "macros.ftlh">
<@htmlBase "Configure database" siteName>
<form id="configDatabase" method="post" action="/config">
  <@databaseConfigFormBase config />
  <input type="submit">
</form>
</@htmlBase>