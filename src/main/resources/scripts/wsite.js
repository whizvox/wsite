function $(id) {
  return document.getElementById(id);
}

function formatBytesSize(num) {
  if (num < 1000) {
    return num.toString() + " B";
  }
  if (num < 1000000) {
    return (num / 1000).toFixed(1).toString() + " KB";
  }
  if (num < 1000000000) {
    return (num / 1000000).toFixed(1).toString() + " MB";
  }
  return (num / 1000000000).toFixed(1).toString() + " GB";
}

function goto(url) {
  window.location.href = url;
}

function getSubmitButton() {
  let elements = document.querySelectorAll("[type=submit]");
  return elements[0];
}

function scrollToBottom(e) {
  e.scrollTop = e.scrollHeight - e.clientHeight;
}

function insertTextAndAutoScroll(e, text) {
  let isScrolledToBottom = e.scrollHeight - e.clientHeight <= e.scrollTop + 1;
  e.insertAdjacentHTML("beforeend", text);
  if (isScrolledToBottom) {
    scrollToBottom(e);
  }
}

function toggleSubmit(d) {
  let submitBtn = getSubmitButton();
  submitBtn.disabled = d;
}

function displayError(msg) {
  alert("Error: " + msg);
}

function deleteTableRows(table) {
  while (table.rows.length > 1) {
    table.deleteRow(1);
  }
}

function deleteAllChildNodes(e) {
  let child = e.lastElementChild;
  while (child) {
    e.removeChild(child);
    child = e.lastElementChild;
  }
}

function encodeMap(map) {
  if (map === null) {
    return "";
  }
  let body = "";
  for (let entry of map.entries()) {
    body += encodeURIComponent(entry[0]) + "=" + encodeURIComponent(entry[1]) + "&";
  }
  return body;
}

function addLoginToken(map) {
  map.set("token", Cookies.get("login"));
}

function handleAsyncJsonRequest(location, body, callback, method, contentType) {
  let req = new XMLHttpRequest();
  if (method === undefined) {
    method = "get";
  }
  if (contentType === undefined) {
    contentType = "application/x-www-form-urlencoded";
  }
  let emptyBody = method.toLowerCase() === "get" || method.toLowerCase() === "head";
  // GET and HEAD requests require that arguments be passed via the URL instead of the body
  if (emptyBody && body !== null) {
    location += "?" + body;
  }
  req.open(method, location);
  req.setRequestHeader("Content-Type", contentType);
  req.onreadystatechange = function() {
    if (this.readyState === XMLHttpRequest.DONE) {
      let contentType = req.getResponseHeader("Content-Type");
      if (contentType !== "application/json") {
        console.log("ERROR: Expected a JSON result, got " + contentType + " instead\n" + this.responseText);
      } else {
        callback(JSON.parse(this.responseText));
      }
    }
  };
  if (emptyBody) {
    req.send(null);
  } else {
    req.send(body);
  }
}

function confirmUserEmail() {
  let eEmail = $("userEmail");
  let eEmailConfirm = $("userConfirmEmail");
  if (eEmail.value !== eEmailConfirm.value) {
    eEmailConfirm.setCustomValidity("Email addresses must match");
  } else {
    eEmailConfirm.setCustomValidity("");
  }
}

function confirmUserPassword() {
  let ePassword = $("userPassword");
  let ePasswordConfirm = $("userConfirmPassword");
  if (ePassword.value !== ePasswordConfirm.value) {
    ePasswordConfirm.setCustomValidity("Passwords must match");
  } else {
    ePasswordConfirm.setCustomValidity("");
  }
}

function checkUsernameAvailable() {
  let eUsername = $("userUsername");
  let args = new Map();
  args.set("username", eUsername.value);
  handleAsyncJsonRequest("/api/user/exists", encodeMap(args), function(res) {
    if (res.success) {
      if (!res.exists) {
        eUsername.setCustomValidity("");
      } else {
        eUsername.setCustomValidity("That username is already taken!");
      }
    } else {
      displayError(res.message);
    }
  });
}

function checkEmailAvailable() {
  let eEmail = $("userEmail");
  let args = new Map();
  args.set("email", eEmail.value);
  handleAsyncJsonRequest("/api/user/exists", encodeMap(args), function(res) {
    if (res.success) {
      if (!res.exists) {
        eEmail.setCustomValidity("");
      } else {
        eEmail.setCustomValidity("That email address is already taken");
      }
    } else {
      displayError(res.message);
    }
  });
}

function checkPathAvailable(validIfExists) {
  let ePath = $("path");
  let args = new Map();
  args.set("path", ePath.value);
  handleAsyncJsonRequest("/api/page/exists", encodeMap(args), function(res) {
    if (res.success) {
      if (validIfExists) {
        if (res.exists) {
          ePath.setCustomValidity("");
        } else {
          ePath.setCustomValidity("That path is already being used");
        }
      } else {
        if (!res.exists) {
          ePath.setCustomValidity("");
        } else {
          ePath.setCustomValidity("That path could not be found");
        }
      }
    } else {
      displayError(res.message)
    }
  });
}

function checkEnableSsl() {
  let enableSsl = $("enableSsl").checked;
  $("keystoreFile").disabled = !enableSsl;
  $("keystorePassword").disabled = !enableSsl;
  $("truststoreFile").disabled = !enableSsl;
  $("truststorePassword").disabled = !enableSsl;
}

function checkEnableSmtp() {
  let enableSmtp = $("enableSmtp").checked;
  $("smtpHost").disabled = !enableSmtp;
  $("smtpFrom").disabled = !enableSmtp;
  $("smtpUser").disabled = !enableSmtp;
  $("smtpPassword").disabled = !enableSmtp;
}

function updatePageInfo() {
  let eOrigPath = $("origPath");
  let args = new Map();
  args.set("path", eOrigPath.value);
  handleAsyncJsonRequest("/api/page/fetch", encodeMap(args), function(res) {
    if (!res.hasOwnProperty("success")) {
      let ePath = $("path");
      let eTitle = $("title");
      let eContents = $("contents");
      let eSyntax = $("syntax");
      ePath.value = res.path;
      eTitle.value = res.title;
      eContents.value = atob(res.contents);
      eSyntax.value = res.syntax.toUpperCase();
      eOrigPath.setCustomValidity("");
    } else {
      if (res.message === "PAGE_PATH_NOT_FOUND") {
        eOrigPath.setCustomValidity("Path does not exist");
      } else {
        eOrigPath.setCustomValidity("");
        displayError(res.message);
      }
    }
  });
}

function updateUserInfo() {
  let eId = $("userId");
  let eUsername = $("userUsername");
  let eEmail = $("userEmail");
  let willFillIn = true;
  let args = new Map();
  if (eId.value !== "") {
    args.set("id", eId.value);
  } else if (eUsername.value !== "") {
    args.set("username", eUsername.value);
  } else if (eEmail.value !== "") {
    args.set("email", eEmail.value);
  } else {
    willFillIn = false;
  }
  addLoginToken(args);
  if (willFillIn) {
    handleAsyncJsonRequest("/api/user/fetch", encodeMap(args), function(res) {
      if (!res.hasOwnProperty("success")) {
        eId.value = res.id;
        eUsername.value = res.username;
        eEmail.value = res.emailAddress;
        $("userOperator").checked = res.operator;
        eId.setCustomValidity("");
        eUsername.setCustomValidity("");
        eEmail.setCustomValidity("");
      } else if (res.message === "USER_INVALID_QUERY") {
        if (eId.value !== "") {
          eId.setCustomValidity("That ID does not exist");
        } else if (eUsername.value !== "") {
          eUsername.setCustomValidity("That username does not exist");
        } else if (eEmail.value !== "") {
          eEmail.setCustomValidity("That email address does not exist");
        }
      } else {
        displayError(res.message);
      }
    });
  }
}

function updateAssetInfo() {
  let args = new Map();
  args.set("path", $("path").value);
  args.set("root", $("root").checked);
  handleAsyncJsonRequest("/api/asset/fetch", encodeMap(args), function(res) {
    if (!res.hasOwnProperty("success")) {
      $("contents").value = atob(res.contents);
    } else {
      displayError(res.message);
    }
  });
}

function insertPages(pagesElement, currentPage, maxPages, params) {
  deleteAllChildNodes(pagesElement);

  if (currentPage > 0) {
    if (currentPage > 1) {
      let firstPage = document.createElement("a");
      params.set("page", 0);
      firstPage.setAttribute("href", window.location.href.split("?")[0] + "?" + params.toString());
      firstPage.setAttribute("class", "page firstPage");
      firstPage.appendChild(document.createTextNode(0));
      pagesElement.appendChild(firstPage);
    }
    let prevPage = document.createElement("a");
    params.set("page", currentPage - 1);
    prevPage.setAttribute("href", window.location.href.split("?")[0] + "?" + params.toString());
    prevPage.setAttribute("class", "page prevPage");
    prevPage.appendChild(document.createTextNode(currentPage - 1));
    pagesElement.appendChild(prevPage);
  }

  let currPage = document.createElement("a");
  params.set("page", currentPage);
  currPage.setAttribute("href", window.location.href.split("?")[0] + "?" + params.toString());
  currPage.setAttribute("class", "page currentPage");
  currPage.appendChild(document.createTextNode(currentPage));
  pagesElement.appendChild(currPage);

  if (currentPage < maxPages) {
    let nextPage = document.createElement("a");
    params.set("page", +currentPage + 1);
    nextPage.setAttribute("href", window.location.href.split("?")[0] + "?" + params.toString());
    nextPage.setAttribute("class", "page nextPage");
    nextPage.appendChild(document.createTextNode(+currentPage + 1));
    pagesElement.appendChild(nextPage);
    if (currentPage < maxPages - 1) {
      let lastPage = document.createElement("a");
      params.set("page", maxPages);
      lastPage.setAttribute("href", window.location.href.split("?")[0] + "?" + params.toString());
      lastPage.setAttribute("class", "page lastPage");
      lastPage.appendChild(document.createTextNode(maxPages));
      pagesElement.appendChild(lastPage);
    }
  }
}

function updateUsersList(init) {
  let params = new URLSearchParams(window.location.search);
  let limit;
  let order;
  let descending;
  if (!init) {
    limit = +$("usersListLimit").value;
    params.set("limit", limit);
    order = $("usersListOrder").value;
    params.set("order", order);
    descending = $("usersListDesc").checked;
    params.set("desc", descending);
  } else {
    limit = 20;
    if (params.has("limit")) {
      limit = +params.get("limit");
    }
    $("usersListLimit").value = limit;
    order = "username";
    if (params.has("order")) {
      order = params.get("order").toLowerCase();
    }
    $("usersListOrder").value = order.toLowerCase();
    descending = false;
    if (params.has("desc")) {
      descending = params.get("desc").toLowerCase() === "true";
    }
    $("usersListDesc").checked = descending;
  }
  if (limit > 100) {
    limit = 100;
  } else if (limit < 5) {
    limit = 5;
  }
  let page = 0;
  if (params.has("page")) {
    page = +params.get("page");
  }
  let args = new Map();
  args.set("limit", limit);
  args.set("page", page);
  args.set("order", order);
  args.set("desc", descending);
  addLoginToken(args);
  handleAsyncJsonRequest("/api/user/list", encodeMap(args), function(res) {
    if (res.hasOwnProperty("success")) {
      displayError("Could not retrieve list of users: " + res.message);
    } else {
      let usersList = $("usersList");
      deleteTableRows(usersList);
      for (let i = 0; i < res.length; i++) {
        let user = res[i];
        let row = usersList.insertRow();
        row.insertCell().appendChild(document.createTextNode(user.id));
        row.insertCell().appendChild(document.createTextNode(user.username));
        row.insertCell().appendChild(document.createTextNode(user.email));
        row.insertCell().appendChild(document.createTextNode(user.operator));
        row.insertCell().appendChild(document.createTextNode(user.created));
      }
    }
  });
  handleAsyncJsonRequest("/api/user/count", null, function(res) {
    if (res.hasOwnProperty("success")) {
      displayError("Count not retrieve count of users: " + res.message);
    } else {
      let pages = $("pages");
      let count = res.count;
      let maxPages = Math.floor(count / limit);
      insertPages(pages, page, maxPages, params);
    }
  });
}

function updatePagesList(init) {
  let params = new URLSearchParams(window.location.search);
  let limit;
  let order;
  let descending;
  if (!init) {
    limit = +$("listPagesLimit").value;
    params.set("limit", limit);
    order = $("listPagesOrder").value;
    params.set("order", order);
    descending = $("listPagesDesc").checked;
    params.set("desc", descending);
  } else {
    limit = 20;
    if (params.has("limit")) {
      limit = +params.get("limit");
    }
    $("listPagesLimit").value = limit;
    order = "path";
    if (params.has("order")) {
      order = params.get("order");
    }
    $("listPagesOrder").value = order;
    descending = false;
    if (params.has("desc")) {
      descending = params.get("desc").toLowerCase() === "true";
    }
    $("listPagesDesc").checked = descending;
  }
  let page = 0;
  if (params.has("page")) {
    page = +params.get("page");
  }

  let args = new Map();
  args.set("limit", limit);
  args.set("order", order);
  args.set("desc", descending);
  args.set("page", page);
  addLoginToken(args);
  handleAsyncJsonRequest("/api/page/list", encodeMap(args), function(res) {
    if (res.hasOwnProperty("success")) {
      displayError(res.message);
    } else {
      let pagesList = $("pagesList");
      deleteTableRows(pagesList);
      for (let i = 0; i < res.length; i++) {
        let page = res[i];
        let row = pagesList.insertRow();
        row.insertCell().appendChild(document.createTextNode(page.path));
        row.insertCell().appendChild(document.createTextNode(page.title));
        row.insertCell().appendChild(document.createTextNode(page.contentLength));
        row.insertCell().appendChild(document.createTextNode(page.syntax));
        row.insertCell().appendChild(document.createTextNode(page.published));
        let lastEditedNode;
        if (page.lastEdited === null) {
          lastEditedNode = document.createElement("span");
          lastEditedNode.setAttribute("class", "nullValue");
        } else {
          lastEditedNode = document.createTextNode(page.lastEdited);
        }
        row.insertCell().appendChild(lastEditedNode);
      }
    }
  });
  handleAsyncJsonRequest("/api/page/count", null, function(res) {
    if (res.hasOwnProperty("success")) {
      displayError("Could not retrieve page count" + res.message);
    } else {
      let pages = $("pages");
      let count = res.count;
      let maxPages = Math.floor(count / limit);
      insertPages(pages, page, maxPages, params);
    }
  });
}

function updateAssetsList() {
  let args = new Map();
  addLoginToken(args);
  handleAsyncJsonRequest("/api/asset/list", encodeMap(args), function(res) {
    if (res.hasOwnProperty("success")) {
      displayError("Could not retrieve assets list: " + res.message);
    } else {
      let assets = $("assetsList");
      deleteTableRows(assets);
      for (let i = 0; i < res.length; i++) {
        let asset = res[i];
        let row = assets.insertRow();
        row.insertCell().appendChild(document.createTextNode(asset.path));
        row.insertCell().appendChild(document.createTextNode(formatBytesSize(+asset.size)));
        row.insertCell().appendChild(document.createTextNode(asset.protect));
        let uploadedNode;
        if (asset.uploaded === null) {
          uploadedNode = document.createElement("span");
          uploadedNode.setAttribute("class", "nullValue");
        } else {
          uploadedNode = document.createTextNode(asset.uploaded);
        }
        row.insertCell().appendChild(uploadedNode);
        let lastEditedNode;
        if (asset.lastEdited === null) {
          lastEditedNode = document.createElement("span");
          lastEditedNode.setAttribute("class", "nullValue");
        } else {
          lastEditedNode = document.createTextNode(asset.lastEdited);
        }
        row.insertCell().appendChild(lastEditedNode);
      }
    }
  });
}

function reloadProtectedAssets() {
  let args = new Map();
  addLoginToken(args);
  handleAsyncJsonRequest("/api/asset/reload", encodeMap(args), function(res) {
    if (!res.success) {
      displayError(res.message);
    }
  }, "post");
}
