<#include "macros.ftlh">
<@htmlBase "Edit page" siteName>
<form id="editPage" method="post">
  <label for="origPath">Original path</label>
  <input type="text" id="origPath" name="origPath">
  <button onclick="updatePageInfo();return false;">Fill in</button><br>
  <@pageFormBase "editPage" true />
  <input type="submit">
</form>
</@htmlBase>
