<#include "macros.ftlh">
<@htmlBase "Edit asset" siteName>
<form id="editAsset" method="post">
  <label for="path">Asset path</label>
  <input type="text" id="path" name="path"><br>
  <label for="root">Located in root</label>
  <input type="checkbox" id="root" name="root" value="true"><br>
  <button onclick="updateAssetInfo();return false;">Fill in</button><br>
  <label for="changePath">Change path</label>
  <input type="checkbox" id="changePath" onchange="hideShowNewOptions()"><br>
  <label for="newPath" id="newPathLabel">New asset path</label>
  <input type="text" id="newPath" name="newPath" onchange="checkExists()"><br>
  <label for="newRoot" id="newRootLabel">Upload to root</label>
  <input type="checkbox" id="newRoot" name="newRoot" value="true" onchange="checkExists()"><br>
  <label for="contents">Contents</label>
  <textarea form="editAsset" id="contents" name="contents" rows="30" cols="100"></textarea><br>
  <input type="submit">
</form>
<script>
  function hideShowNewOptions() {
    let checked = $("changePath").checked;
    $("newPathLabel").hidden = !checked;
    $("newPath").hidden = !checked;
    $("newRootLabel").hidden = !checked;
    $("newRoot").hidden = !checked;
    if (!checked) {
      $("newPath").value = "";
      $("newRoot").checked = false;
    }
  }
  function checkExists() {
    if ($("changePath").checked) {
      let args2 = new Map();
      let eNewPath = $("newPath");
      let eNewRoot = $("newRoot");
      let newPath = eNewPath.value;
      let newRoot = eNewRoot.checked;
      if (newPath !== "") {
        args2.set("path", newPath);
        args2.set("root", newRoot);
        handleAsyncJsonRequest("/api/asset/exists", encodeMap(args2), function (res) {
          if (res.hasOwnProperty("success")) {
            displayError("Could not retrieve whether an asset exists or not: " + res.message);
          } else {
            if (res.exists) {
              eNewPath.setCustomValidity("Path conflict");
              eNewRoot.setCustomValidity("Path conflict");
            } else {
              eNewPath.setCustomValidity("");
              eNewRoot.setCustomValidity("");
            }
          }
        });
      }
    }
  }
  hideShowNewOptions();
</script>
</@htmlBase>
