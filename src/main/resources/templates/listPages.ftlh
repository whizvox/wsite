<#include "macros.ftlh">
<@htmlBase "List pages" siteName>
<table id="pagesList">
  <tr>
    <th>Path</th>
    <th>Title</th>
    <th>Contents length</th>
    <th>Syntax</th>
    <th>Published</th>
    <th>Last edited</th>
  </tr>
</table>
<div id="pagesListView">
  <label for="listPagesOrder">Order by</label>
  <select id="listPagesOrder">
    <option value="path">Path</option>
    <option value="title">Title</option>
    <option value="contents_length">Contents length</option>
    <option value="syntax">Syntax</option>
    <option value="published">Published</option>
    <option value="last_edited">Last edited</option>
  </select>
  <label for="listPagesDesc">Descending order</label>
  <input type="checkbox" id="listPagesDesc" value="true">
  <label for="listPagesLimit">Limit</label>
  <input type="number" id="listPagesLimit">
  <button onclick="updatePagesList(false)">Refresh</button>
</div>
<div id="pages"></div>
<script>
  updatePagesList(true);
</script>
</@htmlBase>