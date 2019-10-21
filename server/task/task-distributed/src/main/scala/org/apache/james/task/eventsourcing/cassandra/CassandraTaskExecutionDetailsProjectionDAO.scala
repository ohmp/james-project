/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/
package org.apache.james.task.eventsourcing.cassandra

import java.util.Optional

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto, select}
import com.datastax.driver.core.{BatchStatement, Row, Session}
import javax.inject.Inject
import org.apache.james.backends.cassandra.init.{CassandraTypesProvider, CassandraZonedDateTimeModule}
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor
import org.apache.james.server.task.json.JsonTaskAdditionalInformationSerializer
import org.apache.james.task.eventsourcing.cassandra.CassandraTaskExecutionDetailsProjectionTable._
import org.apache.james.task.{Hostname, TaskExecutionDetails, TaskId, TaskManager, TaskType}
import reactor.core.publisher.{Flux, Mono}

class CassandraTaskExecutionDetailsProjectionDAO @Inject()(session: Session, typesProvider: CassandraTypesProvider, jsonTaskAdditionalInformationSerializer: JsonTaskAdditionalInformationSerializer) {
  private val cassandraAsyncExecutor = new CassandraAsyncExecutor(session)
  private val dateType = typesProvider.getDefinedUserType(CassandraZonedDateTimeModule.ZONED_DATE_TIME)

  private val insertStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(TYPE, bindMarker(TYPE))
    .value(STATUS, bindMarker(STATUS))
    .value(SUBMITTED_DATE, bindMarker(SUBMITTED_DATE))
    .value(SUBMITTED_NODE, bindMarker(SUBMITTED_NODE)))

  private val insertFailedDateStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(FAILED_DATE, bindMarker(FAILED_DATE)))

  private val insertCancelRequestedNodeStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(CANCEL_REQUESTED_NODE, bindMarker(CANCEL_REQUESTED_NODE)))

  private val insertCanceledDateStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(CANCELED_DATE, bindMarker(CANCELED_DATE)))

  private val insertCompletedDateStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(COMPLETED_DATE, bindMarker(COMPLETED_DATE)))

  private val insertStartedDateStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(STARTED_DATE, bindMarker(STARTED_DATE)))

  private val insertRanNodeStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(RAN_NODE, bindMarker(RAN_NODE)))

  private val insertAdditionalInformationStatement = session.prepare(insertInto(TABLE_NAME)
    .value(TASK_ID, bindMarker(TASK_ID))
    .value(ADDITIONAL_INFORMATION, bindMarker(ADDITIONAL_INFORMATION)))

  private val selectStatement = session.prepare(select().from(TABLE_NAME)
    .where(QueryBuilder.eq(TASK_ID, bindMarker(TASK_ID))))

  private val listStatement = session.prepare(select().from(TABLE_NAME))

  def saveDetails(details: TaskExecutionDetails): Mono[Void] = {
    val batchStatement = new BatchStatement()
    val boundInsertStatement = insertStatement.bind()
      .setUUID(TASK_ID, details.getTaskId.getValue)
      .setString(TYPE, details.getType.asString())
      .setString(STATUS, details.getStatus.getValue)
      .setUDTValue(SUBMITTED_DATE, CassandraZonedDateTimeModule.toUDT(dateType, details.getSubmittedDate))
      .setString(SUBMITTED_NODE, details.getSubmittedNode.asString)

    batchStatement.add(boundInsertStatement)

    details.getAdditionalInformation
        .ifPresent(information => batchStatement.add(
          insertAdditionalInformationStatement.bind()
            .setUUID(TASK_ID, details.getTaskId.getValue)
            .setString(ADDITIONAL_INFORMATION, jsonTaskAdditionalInformationSerializer.serialize(information))))

    details.getStartedDate
      .ifPresent(startedDate => batchStatement.add(
        insertStartedDateStatement.bind()
          .setUUID(TASK_ID, details.getTaskId.getValue)
          .setUDTValue(STARTED_DATE, CassandraZonedDateTimeModule.toUDT(dateType, startedDate))))

    details.getFailedDate
        .ifPresent(failedDate => batchStatement.add(
          insertFailedDateStatement.bind()
            .setUUID(TASK_ID, details.getTaskId.getValue)
            .setUDTValue(FAILED_DATE, CassandraZonedDateTimeModule.toUDT(dateType, failedDate))))

    details.getCanceledDate
        .ifPresent(canceledDate => batchStatement.add(
          insertCanceledDateStatement.bind()
            .setUUID(TASK_ID, details.getTaskId.getValue)
            .setUDTValue(CANCELED_DATE, CassandraZonedDateTimeModule.toUDT(dateType, canceledDate))))

    details.getCompletedDate
        .ifPresent(completedDate => batchStatement.add(
          insertCompletedDateStatement.bind()
            .setUUID(TASK_ID, details.getTaskId.getValue)
            .setUDTValue(COMPLETED_DATE, CassandraZonedDateTimeModule.toUDT(dateType, completedDate))))

    details.getRanNode
      .ifPresent(ranNode => batchStatement.add(
        insertRanNodeStatement.bind()
        .setUUID(TASK_ID, details.getTaskId.getValue)
        .setString(RAN_NODE, ranNode.asString)))

    details.getCancelRequestedNode
      .ifPresent(cancelRequestNode => batchStatement.add(
        insertCancelRequestedNodeStatement.bind()
        .setUUID(TASK_ID, details.getTaskId.getValue)
        .setString(CANCEL_REQUESTED_NODE, cancelRequestNode.asString)))

    cassandraAsyncExecutor.executeVoid(batchStatement);
  }

  def readDetails(taskId: TaskId): Mono[TaskExecutionDetails] = cassandraAsyncExecutor
    .executeSingleRow(selectStatement.bind().setUUID(TASK_ID, taskId.getValue))
    .map(readRow)

  def listDetails(): Flux[TaskExecutionDetails] = cassandraAsyncExecutor
    .executeRows(listStatement.bind())
    .map(readRow)

  private def readRow(row: Row): TaskExecutionDetails = {
    val taskType = TaskType.of(row.getString(TYPE))
    new TaskExecutionDetails(
      taskId = TaskId.fromUUID(row.getUUID(TASK_ID)),
      `type` = TaskType.of(row.getString(TYPE)),
      status = TaskManager.Status.fromString(row.getString(STATUS)),
      submittedDate = CassandraZonedDateTimeModule.fromUDT(row.getUDTValue(SUBMITTED_DATE)),
      submittedNode = Hostname(row.getString(SUBMITTED_NODE)),
      startedDate = CassandraZonedDateTimeModule.fromUDTOptional(row.getUDTValue(STARTED_DATE)),
      ranNode = Optional.ofNullable(row.getString(RAN_NODE)).map(Hostname(_)),
      completedDate = CassandraZonedDateTimeModule.fromUDTOptional(row.getUDTValue(COMPLETED_DATE)),
      canceledDate = CassandraZonedDateTimeModule.fromUDTOptional(row.getUDTValue(CANCELED_DATE)),
      cancelRequestedNode = Optional.ofNullable(row.getString(CANCEL_REQUESTED_NODE)).map(Hostname(_)),
      failedDate = CassandraZonedDateTimeModule.fromUDTOptional(row.getUDTValue(FAILED_DATE)),
      additionalInformation = () => deserializeAdditionalInformation(taskType, row))
  }

  private def deserializeAdditionalInformation(taskType: TaskType, row: Row): Optional[TaskExecutionDetails.AdditionalInformation] = {
    Optional.ofNullable(row.getString(ADDITIONAL_INFORMATION))
      .map(additionalInformation => jsonTaskAdditionalInformationSerializer.deserialize(additionalInformation))
  }
}
