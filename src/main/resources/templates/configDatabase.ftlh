<#include "macros.ftlh">
<@htmlBase "Configure database" siteName>
<form id="configDatabase" name="configDatabase" method="post">
  <@databaseConfigForm config />
  <input type="submit">
</form>
</@htmlBase>