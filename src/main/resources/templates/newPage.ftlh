<#include "macros.ftlh">
<@htmlBase "Create new page" siteName>
<form id="editPage" name="editPage" method="post">
  <label for="path">Path</label>
  <input id="path" name="path"><br>
  <label for="title">Title</label>
  <input id="title" name="title"><br>
  <label for="syntax">Syntax</label>
  <select form="editPage" id="syntax" name="syntax">
    <option value="markdown">Markdown</option>
    <option value="embedded">Embedded Plain Text</option>
    <option value="html_limited_embedded">Embedded Limited HTML</option>
    <option value="html_embedded">Embedded Raw HTML</option>
    <option value="plain">Plain Text</option>
    <option value="html_limited">Limited HTML</option>
    <option value="html">Raw HTML</option>
  </select><br>
  <label for="contents">Contents</label>
  <textarea form="editPage" id="contents" name="contents" rows="50" cols="100"></textarea><br>
  <input type="submit">
</form>
</@htmlBase>