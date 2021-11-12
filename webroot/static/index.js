const headers = {
  'Accept': 'application/json, text/plain, */*',
  'Content-Type': 'application/json'
};

let action;

const ACTION_STATES = {
  CREATE: 'CREATE',
  MODIFY: 'MODIFY',
  DELETE: 'DELETE'
};

const servicesContainer = $('#service-list');
const idInput = $('#modal-id');
const urlInput = $('#modal-url');
const nameInput = $('#modal-name');
const modalTitle = $('#modal-title');
const buttonTilte = $( '#action' );

$(document).ready(function() {
  populateServicesTable();
  setInterval(populateServicesTable, 5000);


  $('#create-modal-btn').click(function () {
    modalTitle.text('Create Service');
    nameInput.attr('disabled', false);
    nameInput.attr('placeholder', '');
    urlInput.attr('disabled', false);
    urlInput.attr('placeholder', '');
    urlInput.val('');
    nameInput.val('');
    buttonTilte.text("Create");
    action = ACTION_STATES.CREATE;
  });

  $('#action').click(function () {
    const url = urlInput.val();
    const name = nameInput.val();
    const id = idInput.val();

    // Validation
    urlInput.css({"border-color": ""});
    nameInput.css({"border-color": ""});
    let isUrlValid = /^(http|https):\/\/[^ "]+$/.test(url);
    let isNameValid = !(name.length === 0);
    if (!isNameValid) {
      nameInput.css({"border-color": "red"});
      return false;
    }
    if (!isUrlValid) {
      urlInput.css({"border-color": "red"});
      return false;
    }

    const method = {
      MODIFY: 'PATCH',
      CREATE: 'POST',
      DELETE: 'DELETE'
    }[action];

    const data = {url, name, id};
    const options = {
      headers,
      method,
      body: JSON.stringify(data)
    };

    fetch('/service', options)
      .then(console.log("Method: " + options.method + " Resuest Body: " + options.body))
      .then(function (response) {
        console.log(response.status);
        if (response.status === 200) {
          populateServicesTable()
          if (options.method === 'POST')
            notify("Successfully Add New Service (" + url + ")", "success");
          else if (options.method === 'PATCH')
            notify("Successfully Modified Service (" + url + ")", "success");
          else if (options.method === 'DELETE')
            notify("Successfully Deleted Service (" + url + ")", "success");
        } else {
          notify("Something goes wrong!", "fail");
        }
      });

  });

});

const notify = (text , type) => {
  console.log(type)
  $(".notify").toggleClass("active");
  if (type === "fail")
    document.getElementById("notify").style.backgroundColor = "red";
  $("#notifyType").text(text);

  setTimeout(function(){
    $(".notify").removeClass("active");
    $("#notifyType").removeClass("success");
  },3000);
}


const populateServicesTable = () => {
  servicesContainer.empty();
  let servicesRequest = new Request('/services');
  fetch(servicesRequest)
    .then(function(response) { return response.json(); })
    .then(function(response) {
      let serviceList = response.message
      const table = $('<table id="table">');
      table.addClass('table table-hover');
      const headers = ['Id', 'Name', 'URL', 'Status', 'Update Time', 'Create Time', 'Modified Time', 'Modify', 'Delete'];
      const headerRow = $('<tr>');
      table.append(headerRow);
      headers.forEach(h => {
        let header = $('<th>');
        if (h==='Id')
          header = $('<th hidden>');
        header.text(h);
        headerRow.append(header);
      });

      servicesContainer.append(table);

      serviceList.forEach(service => {
        const row = $('<tr>');

        const id = $('<td hidden>');
        id.text(service.id);

        const url = $('<td>');
        url.text(service.url);

        const name = $('<td>');
        name.text(service.name);

        const createdTime = $('<td>');
        createdTime.text(service.created_time);

        const status = $('<td>');
        status.text(service.status);
        if (service.status === 'OK') status.css('color', 'green');
        if (service.status === 'FAIL') {
          row.addClass("table-danger")
          status.css('color', 'red');
        }

        const updatedTime = $('<td>');
        updatedTime.text(service.updated_time);

        const modifiedTime = $('<td>');
        modifiedTime.text(service.modified_time);

        const modifyTableData = $('<td>');
        const modifyBtn = $('<button>');
        modifyBtn.text('Modify');
        modifyBtn.addClass('btn btn-outline-primary');
        modifyBtn.attr('data-target', '#modal');
        modifyBtn.attr('data-toggle', 'modal');


        $(modifyBtn).click(function() {
          modalTitle.text("Modify Service");
          idInput.val(service.id);
          urlInput.val(service.url);
          nameInput.val(service.name);
          idInput.attr('disabled', true);
          urlInput.attr('disabled', false);
          nameInput.attr('disabled', false);
          buttonTilte.text("Modify");
          action = ACTION_STATES.MODIFY;
          urlInput.css({"border-color": ""});
          nameInput.css({"border-color": ""});
        });

        modifyTableData.append(modifyBtn);

        const deleteTableData = $('<td>');
        const deleteBtn = $('<button>');
        deleteBtn.text('Delete');
        deleteBtn.attr('data-target', '#modal');
        deleteBtn.attr('data-toggle', 'modal');
        deleteBtn.addClass('btn btn-outline-danger');
        //deleteBtn.attr('type','button');

        $(deleteBtn).click(function() {
          modalTitle.text("Delete Service");
          idInput.attr('placeholder', service.id);
          urlInput.attr('placeholder', service.url);
          nameInput.attr('placeholder', service.name);
          idInput.val(service.id);
          urlInput.val(service.url);
          nameInput.val(service.name);
          idInput.attr('disabled', true);
          urlInput.attr('disabled', true);
          nameInput.attr('disabled', true);
          buttonTilte.text("Delete");
          action = ACTION_STATES.DELETE;
          urlInput.css({"border-color": ""});
          nameInput.css({"border-color": ""});
        });

        deleteTableData.append(deleteBtn);

        row.append(id);
        row.append(name);
        row.append(url);
        row.append(status);
        row.append(updatedTime);
        row.append(createdTime);
        row.append(modifiedTime);
        row.append(modifyTableData);
        row.append(deleteTableData);

        table.append(row);

      });
    });
}
