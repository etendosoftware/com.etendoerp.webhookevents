OB.SMFWHE = {}

OB.SMFWHE.queueEvent = function (tableId, eventTypeId, eventClass, recordId, callback) {
	// Queues an event
	OB.RemoteCallManager.call('com.smf.webhookevents.actionHandlers.queueEventFromJSActionHandler',
		{ tableId: tableId, eventTypeId: eventTypeId, eventClass: eventClass, recordId: recordId }, {}, callback);
}