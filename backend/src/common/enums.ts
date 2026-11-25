export enum Rol {
  ADMIN = 'ADMIN',
  TECNICO = 'TECNICO',
  USUARIO = 'USUARIO',
}

export enum Prioridad {
  BAJA = 'BAJA',
  MEDIA = 'MEDIA',
  ALTA = 'ALTA',
  CRITICA = 'CRITICA',
}

export enum EstadoIncidencia {
  ABIERTA = 'ABIERTA',
  ASIGNADA = 'ASIGNADA',
  EN_PROGRESO = 'EN_PROGRESO',
  RESUELTA = 'RESUELTA',
  CERRADA = 'CERRADA',
  CANCELADA = 'CANCELADA',
}

export enum TipoSeguimiento {
  CREACION = 'CREACION',
  COMENTARIO = 'COMENTARIO',
  CAMBIO_ESTADO = 'CAMBIO_ESTADO',
  ASIGNACION = 'ASIGNACION',
}
