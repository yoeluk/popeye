package com.popeyepdf

/**
 * Created by yoelusa on 24/03/15.
 */


import spray.json.{JsValue, DefaultJsonProtocol}

case class Result(result: JsValue, id: String)

case class FailedResult(failedMsg: String)

case class InternalError(errorMsg: String)

case class ParseRequest(fileName: String)

//----------------------------------------------
// JSON
//----------------------------------------------

object PopeyeJsonProtocol extends DefaultJsonProtocol {

  implicit val resultFormat = jsonFormat2(Result.apply)

  implicit val FailedResultFormat = jsonFormat1(FailedResult.apply)

  implicit val InternalErrorFormat = jsonFormat1(InternalError.apply)

  implicit val ParseRequestFormat = jsonFormat1(ParseRequest.apply)

}
