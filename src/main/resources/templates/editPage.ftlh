<#include "macros.ftlh">
<@htmlBase "Edit page" siteName>
<script>
  function updatePageData() {
    var http = new XMLHttpRequest();
    var data = new FormData();
    data.append("path", $("origPath").value);
    http.open("GET", "/api/pages/get", true);
    http.onreadystatechange = function() {
      if (http.readyState === 4 && http.status === 200) {
        var page = JSON.parse(http.responseText);
        if (page.hasOwnProperty("success") && !page.success) {
          var eOrigPath = $("origPath");
          eOrigPath.setCustomValidity("Not found");
        }
        var ePath = $('path');
        ePath.value = page.path;
        var eTitle = $('title');
        eTitle.value = page.title;
        var eSyntax = $('syntax');
        eSyntax.value = page.syntax;
        var eContents = $('contents');
        eContents.value = page.contents;
      }
    };
    http.send(data);
  }
</script>
<form id="editPage">
  <label for="origPath">Path</label>
  <input type="text" id="origPath" name="origPath">
  <button onclick="updatePageData()">Fill in</button><br>
  <@pageFormBase "editPage" />
  <input type="submit" value="Submit">
</form>
</@htmlBase>
