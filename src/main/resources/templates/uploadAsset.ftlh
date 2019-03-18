<#include "macros.ftlh">
<@htmlBase "Upload file" siteName>
<form id="uploadAsset" method="post" enctype="multipart/form-data">
  <label for="path">Path</label>
  <input type="text" id="path" name="path"><br>
  <label for="file">File</label>
  <input type="file" id="file" name="file"><br>
  <label for="replace">Replace existing</label>
  <input type="checkbox" id="replace" name="replace" value="true"><br>
  <input type="submit">
</form>
</@htmlBase>