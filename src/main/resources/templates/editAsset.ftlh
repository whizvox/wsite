<#include "macros.ftlh">
<@htmlBase "Edit asset" siteName>
<form id="editAsset" method="post">
  <label for="origPath">Original asset path</label>
  <input type="text" id="origPath" name="origPath">
  <button onclick="updateAssetInfo();return false;">Fill in</button><br>
  <label for="path">Asset path</label>
  <input type="text" id="path" name="path"><br>
  <label for="contents">Contents</label>
  <textarea form="editAsset" id="contents" name="contents" rows="30" cols="100"></textarea><br>
  <input type="submit">
</form>
</@htmlBase>
