<#include "macros.ftlh">
<@htmlBase "Edit page" siteName>
<script>
  function updatePageData() {
    var http = new XMLHttpRequest();
    var data = new FormData();
    data.append('path', document.getElementById('search_query').value);
    http.open('GET', '/api/pages/get', true);
    http.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    http.onreadystatechange = function() {
      if (http.readyState === 4 && http.status === 200) {
        var page = JSON.parse(http.responseText);
        var ePagePath = document.getElementById('path');
        ePagePath.value = page.path;
        var ePageTitle = document.getElementById('title');
        ePageTitle.value = page.title;
        var ePageSyntax = document.getElementById('syntax');
        ePageSyntax.value = page.syntax;
        var ePageContents = document.getElementById('contents');
        ePageContents.value = page.contents;
      }
    };
    http.send(data);
  }
</script>
<form id="editPage" name="editPage">
  <label for="path">Path</label>
  <input type="text" id="path" name="path">
  <button onclick="updatePageData()">Fill in</button><br>
  <label for="title">Title</label>
  <input type="text" id="title" name="title"><br>
  <label for="syntax">Syntax</label>
  <select form="editPage" id="syntax" name="syntax">
    <option value="plain">Plain Text</option>
    <option value="html_limited">Limited HTML</option>
    <option value="html">Raw HTML</option>
  </select><br>
  <label for="contents">Contents</label>
  <textarea form="editPage" id="contents" name="contents" rows="10" cols="40"></textarea>
  <input type="submit" value="Submit">
</form>
</@htmlBase>
