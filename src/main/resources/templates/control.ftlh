<#include "macros.ftlh">
<@htmlBase "Control panel" siteName>
<ul id="controlOptions">
  <li><a href="/control/console">View server log</a></li>
  <li>Assets
    <ul>
      <li><a href="/control/uploadAsset">Upload</a></li>
      <li><a href="/control/editAsset">Edit</a></li>
      <li><a href="/control/deleteAsset">Delete</a></li>
      <li><a href="/control/listAssets">List</a></li>
      <li><a href="javascript:void(0)" onclick="reloadProtectedAssets()">Reload protected</a></li>
    </ul>
  </li>
  <li>Pages
    <ul>
      <li><a href="/control/createPage">Create</a></li>
      <li><a href="/control/editPage">Edit</a></li>
      <li><a href="/control/deletePage">Delete</a></li>
      <li><a href="/control/listPages">List</a></li>
    </ul>
  </li>
  <li>Users
    <ul>
      <li><a href="/control/createUser">Create</a></li>
      <li><a href="/control/editUser">Edit</a></li>
      <li><a href="/control/deleteUser">Delete</a></li>
      <li><a href="/control/listUsers">List</a></li>
    </ul>
  </li>
  <li><a href="/control/config">Edit configuration settings</a></li>
  <li>Power
    <ul>
      <li><a href="/control/restart">Force restart</a></li>
      <li><a href="/control/shutdown">Shutdown</a></li>
    </ul>
  </li>
  <li><a href="/">Back to index</a></li>
</ul>
</@htmlBase>