function $(id) {
  return document.getElementById(id);
}

function goto(url) {
  window.location.href = url;
}

function getSubmitButton() {
  var elements = document.querySelectorAll("[type=submit]");
  return elements[0];
}

function toggleSubmit(d) {
  var submitBtn = getSubmitButton();
  submitBtn.disabled = d;
}

function displayError(msg) {
  alert("Error: " + msg);
}

function handleAsyncJsonRequest(location, formData, callback, method) {
  var req = new XMLHttpRequest();
  if (typeof(method) === "undefined") {
    method = "get";
  }
  var getRequest = method.toLowerCase() === "get";
  // GET requests require that arguments be passed via the URL instead of the body
  if (getRequest) {
    location += "?";
    var i = 0;
    for (var entry of formData.entries()) {
      if (i > 0) {
        location += "&";
      }
      location += encodeURIComponent(entry[0]) + "=" + encodeURIComponent(entry[1]);
      i++;
    }
  }
  req.open(method, location, true);
  req.onreadystatechange = function() {
    if (req.readyState === 4) {
      var contentType = req.getResponseHeader("Content-Type");
      if (contentType !== "application/json") {
        console.log("ERROR: Expected a JSON result, got " + contentType + " instead\n" + req.responseText);
      } else {
        callback(JSON.parse(req.responseText));
      }
    }
  };
  if (getRequest) {
    req.send(null);
  } else {
    req.send(formData);
  }
}

function confirmUserEmail() {
  var eEmail = $("userEmail");
  var eEmailConfirm = $("userConfirmEmail");
  if (eEmail.value !== eEmailConfirm.value) {
    eEmailConfirm.setCustomValidity("Email addresses must match");
  } else {
    eEmailConfirm.setCustomValidity("");
  }
}
function confirmUserPassword() {
  var ePassword = $("userPassword");
  var ePasswordConfirm = $("userConfirmPassword");
  if (ePassword.value !== ePasswordConfirm.value) {
    ePasswordConfirm.setCustomValidity("Passwords must match");
  } else {
    ePasswordConfirm.setCustomValidity("");
  }
}
function checkUsernameAvailable() {
  var eUsername = $("userUsername");
  var data = new FormData();
  data.append("username", eUsername.value);
  handleAsyncJsonRequest("/api/user/exists", data, function(res) {
    if (res.success) {
      if (!res.exists) {
        eUsername.setCustomValidity("");
      } else {
        eUsername.setCustomValidity("That username is already taken!");
      }
    } else {
      // TODO: Think of a better way to handle server-side errors
      displayError(res.message);
    }
  });
}
function checkEmailAvailable() {
  var eEmail = $("userEmail");
  var data = new FormData();
  data.append("email", eEmail.value);
  handleAsyncJsonRequest("/api/user/exists", data, function(res) {
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

function checkPathAvailable(invalidIfExists) {
  var ePath = $("path");
  var data = new FormData();
  data.append("path", ePath.value);
  handleAsyncJsonRequest("/api/page/exists", data, function(res) {
    if (res.success) {
      if (invalidIfExists) {
        if (!res.exists) {
          ePath.setCustomValidity("");
        } else {
          ePath.setCustomValidity("That path is already being used");
        }
      } else {
        if (res.exists) {
          ePath.setCustomValidity("");
        } else {
          ePath.setCustomValidity("That path could not be found");
        }
      }
    } else {
      displayError("Error: " + res.message)
    }
  });
}

function updatePageInfo() {
  var eOrigPath = $("origPath");
  var data = new FormData();
  data.append("path", eOrigPath.value);
  handleAsyncJsonRequest("/api/page/fetch", data, function(res) {
    if (!res.hasOwnProperty("success")) {
      var ePath = $("path");
      var eTitle = $("title");
      var eContents = $("contents");
      var eSyntax = $("syntax");
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
        displayError("Error: " + res.message);
      }
    }
  });
}

function updateUserInfo() {
  var eId = $("userId");
  var eUsername = $("userUsername");
  var eEmail = $("userEmail");
  var willFillIn = true;
  var data = new FormData();
  if (eId.value !== "") {
    data.append("id", eId.value);
  } else if (eUsername.value !== "") {
    data.append("username", eUsername.value);
  } else if (eEmail.value !== "") {
    data.append("email", eEmail.value);
  } else {
    willFillIn = false;
  }
  data.append("token", Cookies.get("login"));
  if (willFillIn) {
    handleAsyncJsonRequest("/api/user/fetch", data, function(res) {
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
  var eOrigPath = $("origPath");
  var data = new FormData();
  data.append("path", eOrigPath.value);
  handleAsyncJsonRequest("/api/asset/fetch", data, function(res) {
    if (!res.hasOwnProperty("success")) {
      var ePath = $("path");
      var eContents = $("contents");
      ePath.value = res.path;
      eContents.value = atob(res.contents);
    } else {
      displayError(res.message);
    }
  });
}
