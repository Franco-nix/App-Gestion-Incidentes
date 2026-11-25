import { ArgumentsHost, Catch, ExceptionFilter, HttpStatus, Logger } from '@nestjs/common';
import { Response } from 'express';
import { QueryFailedError } from 'typeorm';

/** Traduce errores de PostgreSQL a respuestas HTTP legibles. */
@Catch(QueryFailedError)
export class DbExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(DbExceptionFilter.name);

  catch(exception: QueryFailedError & { code?: string; detail?: string }, host: ArgumentsHost) {
    const res = host.switchToHttp().getResponse<Response>();
    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let message = 'Error de base de datos';

    switch (exception.code) {
      case '23505': // unique_violation
        status = HttpStatus.CONFLICT;
        message = 'Ya existe un registro con esos datos';
        break;
      case '23503': // foreign_key_violation
        status = HttpStatus.BAD_REQUEST;
        message = 'Referencia a un registro inexistente';
        break;
      case '22P02': // invalid_text_representation
      case '23502': // not_null_violation
        status = HttpStatus.BAD_REQUEST;
        message = 'Datos inválidos';
        break;
      default:
        this.logger.error(exception.message, exception.stack);
    }
    res.status(status).json({ statusCode: status, message, detail: exception.detail });
  }
}
