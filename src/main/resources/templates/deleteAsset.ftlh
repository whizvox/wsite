<#include "macros.ftlh">
<@htmlBase "Delete asset" siteName>
<form id="deleteAsset" method="post">
  <label for="path">Path</label>
  <input type="text" id="path" name="path"><br>
  <label for="root">In root</label>
  <input type="checkbox" id="root" name="root" value="true"><br>
  <input type="submit">
</form>
</@htmlBase>