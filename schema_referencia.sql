--
-- PostgreSQL database dump
--

\restrict JDm4M4mstz5mrwRSxpT3styE9oZzcufkduiEVN41c4cMKO7g5HxYiuQ6JZUnIal

-- Dumped from database version 17.11
-- Dumped by pg_dump version 17.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: fn_validar_asistencia_integridad(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_asistencia_integridad() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_grupo_clase bigint;
    v_grupo_inscripcion bigint;
BEGIN
    SELECT id_grupo INTO v_grupo_clase FROM public.clases WHERE id_clase = NEW.id_clase;
    SELECT id_grupo INTO v_grupo_inscripcion FROM public.inscripciones WHERE id_inscripcion = NEW.id_inscripcion;

    IF v_grupo_clase IS NULL THEN RAISE EXCEPTION 'La clase % no existe', NEW.id_clase; END IF;
    IF v_grupo_inscripcion IS NULL THEN RAISE EXCEPTION 'La inscripción % no existe', NEW.id_inscripcion; END IF;
    IF v_grupo_clase <> v_grupo_inscripcion THEN
        RAISE EXCEPTION 'La clase % pertenece al grupo %, pero la inscripción % pertenece al grupo %', NEW.id_clase, v_grupo_clase, NEW.id_inscripcion, v_grupo_inscripcion;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: fn_validar_calificacion_integridad(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_calificacion_integridad() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_grupo_inscripcion bigint;
    v_grupo_evaluacion bigint;
BEGIN
    SELECT id_grupo INTO v_grupo_inscripcion FROM public.inscripciones WHERE id_inscripcion = NEW.id_inscripcion;
    SELECT id_grupo INTO v_grupo_evaluacion FROM public.evaluaciones WHERE id_evaluacion = NEW.id_evaluacion;

    IF v_grupo_inscripcion IS NULL THEN RAISE EXCEPTION 'La inscripción % no existe', NEW.id_inscripcion; END IF;
    IF v_grupo_evaluacion IS NULL THEN RAISE EXCEPTION 'La evaluación % no existe', NEW.id_evaluacion; END IF;
    IF v_grupo_inscripcion <> v_grupo_evaluacion THEN
        RAISE EXCEPTION 'La inscripción % pertenece al grupo %, pero la evaluación % pertenece al grupo %', NEW.id_inscripcion, v_grupo_inscripcion, NEW.id_evaluacion, v_grupo_evaluacion;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: fn_validar_inscripcion_integridad(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_inscripcion_integridad() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_ciclo_matricula bigint;
    v_ciclo_grupo bigint;
    v_ciclo_periodo bigint;
BEGIN
    SELECT id_ciclo INTO v_ciclo_matricula FROM public.matriculas WHERE id_matricula = NEW.id_matricula;
    SELECT id_ciclo INTO v_ciclo_grupo FROM public.grupos WHERE id_grupo = NEW.id_grupo;

    IF v_ciclo_matricula IS NULL THEN RAISE EXCEPTION 'La matrícula % no existe', NEW.id_matricula; END IF;
    IF v_ciclo_grupo IS NULL THEN RAISE EXCEPTION 'El grupo % no existe', NEW.id_grupo; END IF;
    IF v_ciclo_matricula <> v_ciclo_grupo THEN
        RAISE EXCEPTION 'La matrícula % pertenece al ciclo %, pero el grupo % pertenece al ciclo %', NEW.id_matricula, v_ciclo_matricula, NEW.id_grupo, v_ciclo_grupo;
    END IF;

    IF NEW.id_periodo IS NOT NULL THEN
        SELECT id_ciclo INTO v_ciclo_periodo FROM public.periodos_academicos WHERE id_periodo = NEW.id_periodo;
        IF v_ciclo_periodo IS NULL THEN RAISE EXCEPTION 'El período académico % no existe', NEW.id_periodo; END IF;
        IF v_ciclo_periodo <> v_ciclo_matricula THEN
            RAISE EXCEPTION 'El período % no pertenece al mismo ciclo de la matrícula %', NEW.id_periodo, NEW.id_matricula;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: areas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.areas (
    id_area bigint NOT NULL,
    id_area_padre bigint,
    area character varying(255) NOT NULL
);


--
-- Name: areas_id_area_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.areas ALTER COLUMN id_area ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.areas_id_area_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: asistencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asistencias (
    id_asistencia bigint NOT NULL,
    id_clase bigint NOT NULL,
    id_inscripcion bigint NOT NULL,
    id_estado_asistencia bigint NOT NULL,
    fecha_registro timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: asistencias_id_asistencia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.asistencias ALTER COLUMN id_asistencia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.asistencias_id_asistencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria (
    id_auditoria bigint NOT NULL,
    id_usuario bigint,
    accion character varying(100) NOT NULL,
    tabla character varying(100),
    id_registro bigint,
    fecha_hora timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    direccion_ip character varying(45),
    descripcion text
);


--
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auditoria ALTER COLUMN id_auditoria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.auditoria_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: aulas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aulas (
    id_aula bigint NOT NULL,
    id_edificio bigint NOT NULL,
    id_tipo_aula bigint NOT NULL,
    id_estado_aula bigint NOT NULL,
    codigo_aula character varying(20) NOT NULL,
    capacidad integer NOT NULL,
    CONSTRAINT chk_capacidad_aula CHECK ((capacidad > 0))
);


--
-- Name: aulas_id_aula_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.aulas ALTER COLUMN id_aula ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.aulas_id_aula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: beneficios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beneficios (
    id_beneficio bigint NOT NULL,
    beneficio character varying(150) NOT NULL,
    porcentaje numeric(5,2),
    monto numeric(14,2),
    cum_minimo numeric(5,2),
    fecha_inicio date NOT NULL,
    fecha_fin date,
    id_estado_beneficio bigint NOT NULL,
    CONSTRAINT chk_beneficio_valor CHECK ((((porcentaje IS NOT NULL) AND (porcentaje > (0)::numeric) AND (porcentaje <= (100)::numeric) AND (monto IS NULL)) OR ((monto IS NOT NULL) AND (monto > (0)::numeric) AND (porcentaje IS NULL)))),
    CONSTRAINT chk_cum_minimo CHECK (((cum_minimo IS NULL) OR ((cum_minimo >= (0)::numeric) AND (cum_minimo <= (10)::numeric)))),
    CONSTRAINT chk_fechas_beneficio CHECK (((fecha_fin IS NULL) OR (fecha_fin >= fecha_inicio)))
);


--
-- Name: beneficios_cobro; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beneficios_cobro (
    id_beneficio_cobro bigint NOT NULL,
    id_beneficio bigint NOT NULL,
    id_tipo_cobro bigint NOT NULL
);


--
-- Name: beneficios_cobro_id_beneficio_cobro_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.beneficios_cobro ALTER COLUMN id_beneficio_cobro ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.beneficios_cobro_id_beneficio_cobro_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: beneficios_id_beneficio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.beneficios ALTER COLUMN id_beneficio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.beneficios_id_beneficio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: calificaciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.calificaciones (
    id_calificacion bigint NOT NULL,
    id_inscripcion bigint NOT NULL,
    id_evaluacion bigint NOT NULL,
    id_estado_calificacion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    nota numeric(4,2),
    fecha_registro timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_publicacion timestamp with time zone,
    fecha_modificacion timestamp with time zone,
    CONSTRAINT chk_nota CHECK (((nota IS NULL) OR ((nota >= (0)::numeric) AND (nota <= (10)::numeric))))
);


--
-- Name: calificaciones_id_calificacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.calificaciones ALTER COLUMN id_calificacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.calificaciones_id_calificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: canales_pago; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.canales_pago (
    id_canal_pago bigint NOT NULL,
    canal_pago character varying(80) NOT NULL
);


--
-- Name: canales_pago_id_canal_pago_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.canales_pago ALTER COLUMN id_canal_pago ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.canales_pago_id_canal_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cargos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cargos (
    id_cargo bigint NOT NULL,
    cargo character varying(255) NOT NULL
);


--
-- Name: cargos_estudiante; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cargos_estudiante (
    id_cargo_estudiante bigint NOT NULL,
    id_matricula bigint NOT NULL,
    id_concepto_cobro bigint NOT NULL,
    id_estudiante_beneficio bigint,
    id_estado_cargo bigint NOT NULL,
    numero_cuota integer DEFAULT 1 NOT NULL,
    monto_base numeric(14,2) NOT NULL,
    monto_descuento numeric(14,2) DEFAULT 0 NOT NULL,
    monto_recargo numeric(14,2) DEFAULT 0 NOT NULL,
    monto_total numeric(14,2) NOT NULL,
    fecha_generacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_vencimiento date,
    CONSTRAINT chk_monto_cargo CHECK (((monto_base >= (0)::numeric) AND (monto_descuento >= (0)::numeric) AND (monto_recargo >= (0)::numeric) AND (monto_total >= (0)::numeric))),
    CONSTRAINT chk_numero_cuota CHECK ((numero_cuota > 0))
);


--
-- Name: cargos_estudiante_id_cargo_estudiante_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cargos_estudiante ALTER COLUMN id_cargo_estudiante ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.cargos_estudiante_id_cargo_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cargos_id_cargo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cargos ALTER COLUMN id_cargo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.cargos_id_cargo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: carreras; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carreras (
    id_carrera bigint NOT NULL,
    id_facultad bigint NOT NULL,
    id_nivel bigint NOT NULL,
    id_estado_carrera bigint NOT NULL,
    codigo_carrera character varying(20) NOT NULL,
    nombre_carrera character varying(150) NOT NULL
);


--
-- Name: carreras_id_carrera_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.carreras ALTER COLUMN id_carrera ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.carreras_id_carrera_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: carreras_sedes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carreras_sedes (
    id_carrera_sede bigint NOT NULL,
    id_carrera bigint NOT NULL,
    id_sede bigint NOT NULL
);


--
-- Name: carreras_sedes_id_carrera_sede_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.carreras_sedes ALTER COLUMN id_carrera_sede ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.carreras_sedes_id_carrera_sede_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ciclos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ciclos (
    id_ciclo bigint NOT NULL,
    id_estado_ciclo bigint NOT NULL,
    anio integer NOT NULL,
    numero_ciclo integer NOT NULL,
    codigo_ciclo character varying(20) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    CONSTRAINT chk_ciclos_fechas_coherentes CHECK ((fecha_fin > fecha_inicio)),
    CONSTRAINT chk_ciclos_numero_valido CHECK ((numero_ciclo = ANY (ARRAY[1, 2]))),
    CONSTRAINT chk_fechas_ciclo CHECK ((fecha_fin > fecha_inicio)),
    CONSTRAINT chk_numero_ciclo CHECK ((numero_ciclo > 0))
);


--
-- Name: ciclos_id_ciclo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.ciclos ALTER COLUMN id_ciclo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.ciclos_id_ciclo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: clases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clases (
    id_clase bigint NOT NULL,
    id_grupo bigint NOT NULL,
    fecha_clase date NOT NULL
);


--
-- Name: clases_id_clase_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.clases ALTER COLUMN id_clase ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.clases_id_clase_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: conceptos_cobro; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conceptos_cobro (
    id_concepto_cobro bigint NOT NULL,
    id_ciclo bigint NOT NULL,
    id_tipo_cobro bigint NOT NULL,
    monto_base numeric(14,2) NOT NULL,
    activo boolean DEFAULT true,
    CONSTRAINT chk_monto_base CHECK ((monto_base >= (0)::numeric))
);


--
-- Name: conceptos_cobro_id_concepto_cobro_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.conceptos_cobro ALTER COLUMN id_concepto_cobro ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.conceptos_cobro_id_concepto_cobro_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: departamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departamentos (
    id_departamento bigint NOT NULL,
    nombre character varying(100) NOT NULL
);


--
-- Name: departamentos_id_departamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.departamentos_id_departamento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: departamentos_id_departamento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.departamentos_id_departamento_seq OWNED BY public.departamentos.id_departamento;


--
-- Name: dias_semana; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dias_semana (
    id_dia bigint NOT NULL,
    dia character varying(20) NOT NULL
);


--
-- Name: dias_semana_id_dia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.dias_semana ALTER COLUMN id_dia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.dias_semana_id_dia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: distritos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.distritos (
    id_distrito bigint NOT NULL,
    id_departamento bigint NOT NULL,
    nombre character varying(120) NOT NULL
);


--
-- Name: distritos_id_distrito_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.distritos_id_distrito_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: distritos_id_distrito_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.distritos_id_distrito_seq OWNED BY public.distritos.id_distrito;


--
-- Name: docentes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.docentes (
    id_docente bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_usuario bigint,
    id_sede bigint NOT NULL,
    id_tipo_contratacion bigint NOT NULL,
    id_estado_docente bigint NOT NULL,
    codigo_docente character varying(255) NOT NULL,
    especialidad character varying(255),
    fecha_inicio date DEFAULT CURRENT_DATE NOT NULL,
    fecha_fin date,
    CONSTRAINT chk_docente_fechas CHECK (((fecha_fin IS NULL) OR (fecha_fin >= fecha_inicio)))
);


--
-- Name: docentes_id_docente_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.docentes ALTER COLUMN id_docente ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.docentes_id_docente_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: edificios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.edificios (
    id_edificio bigint NOT NULL,
    id_sede bigint NOT NULL,
    id_estado_edificio bigint NOT NULL,
    codigo_edificio character varying(20) NOT NULL,
    nombre_edificio character varying(100) NOT NULL,
    direccion character varying(200)
);


--
-- Name: edificios_id_edificio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.edificios ALTER COLUMN id_edificio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.edificios_id_edificio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: empleados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.empleados (
    id_empleado bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_usuario bigint,
    id_area bigint NOT NULL,
    id_cargo bigint NOT NULL,
    id_estado_empleado bigint NOT NULL,
    codigo_empleado character varying(255) NOT NULL,
    fecha_ingreso date DEFAULT CURRENT_DATE NOT NULL,
    fecha_fin date,
    id_sede bigint NOT NULL
);


--
-- Name: empleados_id_empleado_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.empleados ALTER COLUMN id_empleado ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.empleados_id_empleado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_asistencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_asistencia (
    id_estado_asistencia bigint NOT NULL,
    estado_asistencia character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_asistencia_id_estado_asistencia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_asistencia ALTER COLUMN id_estado_asistencia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_asistencia_id_estado_asistencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_aula; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_aula (
    id_estado_aula bigint NOT NULL,
    estado_aula character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_aula_id_estado_aula_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_aula ALTER COLUMN id_estado_aula ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_aula_id_estado_aula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_beneficio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_beneficio (
    id_estado_beneficio bigint NOT NULL,
    estado_beneficio character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_beneficio_estudiante; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_beneficio_estudiante (
    id_estado_beneficio_estudiante bigint NOT NULL,
    estado_beneficio_estudiante character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_beneficio_estudiante_id_estado_beneficio_estudiante_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_beneficio_estudiante ALTER COLUMN id_estado_beneficio_estudiante ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_beneficio_estudiante_id_estado_beneficio_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_beneficio_id_estado_beneficio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_beneficio ALTER COLUMN id_estado_beneficio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_beneficio_id_estado_beneficio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_calificacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_calificacion (
    id_estado_calificacion bigint NOT NULL,
    estado_calificacion character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_calificacion_id_estado_calificacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_calificacion ALTER COLUMN id_estado_calificacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_calificacion_id_estado_calificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_cargo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_cargo (
    id_estado_cargo bigint NOT NULL,
    estado_cargo character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_cargo_id_estado_cargo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_cargo ALTER COLUMN id_estado_cargo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_cargo_id_estado_cargo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_carrera; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_carrera (
    id_estado_carrera bigint NOT NULL,
    estado_carrera character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_carrera_id_estado_carrera_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_carrera ALTER COLUMN id_estado_carrera ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_carrera_id_estado_carrera_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_ciclo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_ciclo (
    id_estado_ciclo bigint NOT NULL,
    estado_ciclo character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_ciclo_id_estado_ciclo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_ciclo ALTER COLUMN id_estado_ciclo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_ciclo_id_estado_ciclo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_docente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_docente (
    id_estado_docente bigint NOT NULL,
    estado_docente character varying(255) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_docente_id_estado_docente_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_docente ALTER COLUMN id_estado_docente ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_docente_id_estado_docente_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_edificio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_edificio (
    id_estado_edificio bigint NOT NULL,
    estado_edificio character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_edificio_id_estado_edificio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_edificio ALTER COLUMN id_estado_edificio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_edificio_id_estado_edificio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_empleado; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_empleado (
    id_estado_empleado bigint NOT NULL,
    estado_empleado character varying(255) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_empleado_id_estado_empleado_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_empleado ALTER COLUMN id_estado_empleado ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_empleado_id_estado_empleado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_estudiante; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_estudiante (
    id_estado_estudiante bigint NOT NULL,
    estado_estudiante character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_estudiante_id_estado_estudiante_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_estudiante ALTER COLUMN id_estado_estudiante ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_estudiante_id_estado_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_factura; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_factura (
    id_estado_factura bigint NOT NULL,
    estado_factura character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_factura_id_estado_factura_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_factura ALTER COLUMN id_estado_factura ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_factura_id_estado_factura_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_facultad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_facultad (
    id_estado_facultad bigint NOT NULL,
    estado_facultad character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_facultad_id_estado_facultad_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_facultad ALTER COLUMN id_estado_facultad ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_facultad_id_estado_facultad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_grupo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_grupo (
    id_estado_grupo bigint NOT NULL,
    estado_grupo character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_grupo_id_estado_grupo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_grupo ALTER COLUMN id_estado_grupo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_grupo_id_estado_grupo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_inscripcion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_inscripcion (
    id_estado_inscripcion bigint NOT NULL,
    estado_inscripcion character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_inscripcion_id_estado_inscripcion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_inscripcion ALTER COLUMN id_estado_inscripcion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_inscripcion_id_estado_inscripcion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_materia_estudiante; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_materia_estudiante (
    id_estado_materia bigint NOT NULL,
    estado_materia character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_materia_estudiante_id_estado_materia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_materia_estudiante ALTER COLUMN id_estado_materia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_materia_estudiante_id_estado_materia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_matricula; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_matricula (
    id_estado_matricula bigint NOT NULL,
    estado_matricula character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_matricula_id_estado_matricula_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_matricula ALTER COLUMN id_estado_matricula ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_matricula_id_estado_matricula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_pago; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_pago (
    id_estado_pago bigint NOT NULL,
    estado_pago character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_pago_id_estado_pago_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_pago ALTER COLUMN id_estado_pago ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_pago_id_estado_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_pensum; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_pensum (
    id_estado_pensum bigint NOT NULL,
    estado_pensum character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_pensum_id_estado_pensum_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_pensum ALTER COLUMN id_estado_pensum ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_pensum_id_estado_pensum_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_recargo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_recargo (
    id_estado_recargo bigint NOT NULL,
    estado_recargo character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_recargo_id_estado_recargo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_recargo ALTER COLUMN id_estado_recargo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_recargo_id_estado_recargo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_registro_persona; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_registro_persona (
    id_estado_registro bigint NOT NULL,
    estado_registro character varying(50) NOT NULL,
    descripcion text,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_registro_persona_id_estado_registro_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_registro_persona ALTER COLUMN id_estado_registro ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_registro_persona_id_estado_registro_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_sede; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_sede (
    id_estado_sede bigint NOT NULL,
    estado_sede character varying(255) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_sede_id_estado_sede_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_sede ALTER COLUMN id_estado_sede ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_sede_id_estado_sede_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_solicitud; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_solicitud (
    id_estado_solicitud bigint NOT NULL,
    estado_solicitud character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_solicitud_id_estado_solicitud_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_solicitud ALTER COLUMN id_estado_solicitud ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_solicitud_id_estado_solicitud_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_trayectoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_trayectoria (
    id_estado_trayectoria bigint NOT NULL,
    estado_trayectoria character varying(50) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_trayectoria_id_estado_trayectoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_trayectoria ALTER COLUMN id_estado_trayectoria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_trayectoria_id_estado_trayectoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estados_usuario (
    id_estado_usuario bigint NOT NULL,
    estado_usuario character varying(60) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estados_usuario_id_estado_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estados_usuario ALTER COLUMN id_estado_usuario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estados_usuario_id_estado_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estudiantes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estudiantes (
    id_estudiante bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_usuario bigint,
    id_estado_estudiante bigint NOT NULL,
    carnet character varying(20) NOT NULL,
    fecha_ingreso date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: estudiantes_beneficios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estudiantes_beneficios (
    id_estudiante_beneficio bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_beneficio bigint NOT NULL,
    id_estado_beneficio_estudiante bigint NOT NULL,
    id_usuario_aprueba bigint,
    fecha_solicitud timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_aprobacion timestamp with time zone
);


--
-- Name: estudiantes_beneficios_id_estudiante_beneficio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estudiantes_beneficios ALTER COLUMN id_estudiante_beneficio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estudiantes_beneficios_id_estudiante_beneficio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estudiantes_carreras; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estudiantes_carreras (
    id_estudiante_carrera bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_carrera_sede bigint NOT NULL,
    id_pensum bigint NOT NULL,
    id_estado_trayectoria bigint NOT NULL,
    fecha_inicio date DEFAULT CURRENT_DATE NOT NULL,
    fecha_fin date,
    CONSTRAINT chk_trayectoria_fechas CHECK (((fecha_fin IS NULL) OR (fecha_fin >= fecha_inicio)))
);


--
-- Name: estudiantes_carreras_id_estudiante_carrera_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estudiantes_carreras ALTER COLUMN id_estudiante_carrera ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estudiantes_carreras_id_estudiante_carrera_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estudiantes_id_estudiante_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estudiantes ALTER COLUMN id_estudiante ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.estudiantes_id_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluaciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluaciones (
    id_evaluacion bigint NOT NULL,
    id_grupo bigint NOT NULL,
    id_tipo_evaluacion bigint NOT NULL,
    numero_evaluacion integer NOT NULL,
    fecha_inicio timestamp with time zone NOT NULL,
    fecha_fin timestamp with time zone NOT NULL,
    id_periodo_evaluacion bigint,
    CONSTRAINT chk_fechas_evaluacion CHECK ((fecha_fin >= fecha_inicio)),
    CONSTRAINT chk_numero_evaluacion CHECK ((numero_evaluacion > 0))
);


--
-- Name: evaluaciones_id_evaluacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.evaluaciones ALTER COLUMN id_evaluacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.evaluaciones_id_evaluacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: facturas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.facturas (
    id_factura bigint NOT NULL,
    id_pago bigint NOT NULL,
    id_estado_factura bigint NOT NULL,
    numero_factura character varying(100) NOT NULL,
    fecha_emision timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    nombre_receptor character varying(150) NOT NULL,
    documento_receptor character varying(30),
    correo_receptor character varying(150),
    direccion_receptor text,
    subtotal numeric(14,2) NOT NULL,
    descuento numeric(14,2) DEFAULT 0 NOT NULL,
    recargo numeric(14,2) DEFAULT 0 NOT NULL,
    total numeric(14,2) NOT NULL,
    CONSTRAINT chk_factura_montos CHECK (((subtotal >= (0)::numeric) AND (descuento >= (0)::numeric) AND (recargo >= (0)::numeric) AND (total >= (0)::numeric)))
);


--
-- Name: facturas_id_factura_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.facturas ALTER COLUMN id_factura ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.facturas_id_factura_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: facultades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.facultades (
    id_facultad bigint NOT NULL,
    id_estado_facultad bigint NOT NULL,
    codigo_facultad character varying(20) NOT NULL,
    nombre_facultad character varying(150) NOT NULL
);


--
-- Name: facultades_id_facultad_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.facultades ALTER COLUMN id_facultad ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.facultades_id_facultad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: facultades_sedes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.facultades_sedes (
    id_facultad_sede bigint NOT NULL,
    id_facultad bigint NOT NULL,
    id_sede bigint NOT NULL
);


--
-- Name: facultades_sedes_id_facultad_sede_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.facultades_sedes ALTER COLUMN id_facultad_sede ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.facultades_sedes_id_facultad_sede_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: grupos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grupos (
    id_grupo bigint NOT NULL,
    id_ciclo bigint NOT NULL,
    id_materia bigint NOT NULL,
    id_docente bigint NOT NULL,
    id_sede bigint NOT NULL,
    id_estado_grupo bigint NOT NULL,
    codigo_grupo character varying(50) NOT NULL,
    id_plantilla bigint,
    cupo_maximo integer NOT NULL,
    CONSTRAINT chk_grupos_cupo_maximo CHECK ((cupo_maximo > 0))
);


--
-- Name: grupos_id_grupo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.grupos ALTER COLUMN id_grupo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.grupos_id_grupo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: horarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.horarios (
    id_horario bigint NOT NULL,
    id_grupo bigint NOT NULL,
    id_dia bigint NOT NULL,
    id_modalidad bigint NOT NULL,
    id_aula bigint,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    enlace_virtual character varying(500),
    CONSTRAINT chk_horas CHECK ((hora_fin > hora_inicio))
);


--
-- Name: horarios_id_horario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.horarios ALTER COLUMN id_horario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.horarios_id_horario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inscripciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inscripciones (
    id_inscripcion bigint NOT NULL,
    id_matricula bigint NOT NULL,
    id_grupo bigint NOT NULL,
    id_periodo bigint,
    id_estado_inscripcion bigint NOT NULL,
    fecha_inscripcion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_retiro timestamp with time zone,
    id_estado_materia bigint NOT NULL,
    CONSTRAINT chk_fecha_retiro CHECK (((fecha_retiro IS NULL) OR (fecha_retiro >= fecha_inscripcion)))
);


--
-- Name: inscripciones_id_inscripcion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.inscripciones ALTER COLUMN id_inscripcion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.inscripciones_id_inscripcion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: intentos_acceso; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.intentos_acceso (
    id_intento bigint NOT NULL,
    id_usuario bigint,
    correo character varying(150) NOT NULL,
    fecha_hora timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    exitoso boolean DEFAULT false NOT NULL,
    direccion_ip character varying(45)
);


--
-- Name: intentos_acceso_id_intento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.intentos_acceso ALTER COLUMN id_intento ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.intentos_acceso_id_intento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: materias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materias (
    id_materia bigint NOT NULL,
    codigo_materia character varying(20) NOT NULL,
    nombre_materia character varying(150) NOT NULL,
    unidades_valorativas numeric(4,2) NOT NULL,
    estado_materia boolean DEFAULT true NOT NULL,
    CONSTRAINT chk_unidades_valorativas CHECK ((unidades_valorativas > (0)::numeric))
);


--
-- Name: materias_id_materia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.materias ALTER COLUMN id_materia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.materias_id_materia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: materias_prerrequisitos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materias_prerrequisitos (
    id_prerrequisito bigint NOT NULL,
    id_materia bigint NOT NULL,
    id_materia_requerida bigint NOT NULL,
    CONSTRAINT chk_prerrequisito CHECK ((id_materia <> id_materia_requerida))
);


--
-- Name: materias_prerrequisitos_id_prerrequisito_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.materias_prerrequisitos ALTER COLUMN id_prerrequisito ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.materias_prerrequisitos_id_prerrequisito_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: matriculas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matriculas (
    id_matricula bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_estudiante_carrera bigint NOT NULL,
    id_ciclo bigint NOT NULL,
    id_estado_matricula bigint NOT NULL,
    fecha_matricula timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: matriculas_id_matricula_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.matriculas ALTER COLUMN id_matricula ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.matriculas_id_matricula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: metodos_pago; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.metodos_pago (
    id_metodo_pago bigint NOT NULL,
    metodo_pago character varying(80) NOT NULL
);


--
-- Name: metodos_pago_id_metodo_pago_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.metodos_pago ALTER COLUMN id_metodo_pago ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.metodos_pago_id_metodo_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: modalidades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.modalidades (
    id_modalidad bigint NOT NULL,
    modalidad character varying(50) NOT NULL
);


--
-- Name: modalidades_id_modalidad_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.modalidades ALTER COLUMN id_modalidad ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.modalidades_id_modalidad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: mv_dashboard_gerencial; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.mv_dashboard_gerencial AS
 SELECT id_ciclo,
    codigo_ciclo,
    ( SELECT count(m.id_matricula) AS count
           FROM public.matriculas m
          WHERE ((m.id_ciclo = c.id_ciclo) AND (m.id_estado_matricula = 2))) AS total_matricula,
    ( SELECT count(DISTINCT g.id_docente) AS count
           FROM public.grupos g
          WHERE ((g.id_ciclo = c.id_ciclo) AND (g.id_estado_grupo = 1))) AS total_docentes,
    ( SELECT count(g.id_grupo) AS count
           FROM public.grupos g
          WHERE ((g.id_ciclo = c.id_ciclo) AND (g.id_estado_grupo = 1))) AS total_secciones,
    ( SELECT COALESCE((avg(((( SELECT count(*) AS count
                   FROM public.inscripciones i
                  WHERE ((i.id_grupo = g.id_grupo) AND (i.id_estado_inscripcion = 1))))::double precision / (NULLIF(g.cupo_maximo, 0))::double precision)) * (100)::double precision), (0)::double precision) AS "coalesce"
           FROM public.grupos g
          WHERE ((g.id_ciclo = c.id_ciclo) AND (g.id_estado_grupo = 1))) AS capacidad_promedio,
    ( SELECT COALESCE(sum(ce.monto_total), (0)::numeric) AS "coalesce"
           FROM (public.cargos_estudiante ce
             JOIN public.conceptos_cobro cc ON ((ce.id_concepto_cobro = cc.id_concepto_cobro)))
          WHERE (cc.id_ciclo = c.id_ciclo)) AS total_facturado,
    ( SELECT COALESCE(sum(ce.monto_total), (0)::numeric) AS "coalesce"
           FROM (public.cargos_estudiante ce
             JOIN public.conceptos_cobro cc ON ((ce.id_concepto_cobro = cc.id_concepto_cobro)))
          WHERE ((cc.id_ciclo = c.id_ciclo) AND (ce.id_estado_cargo = 2))) AS total_recaudado,
    CURRENT_TIMESTAMP AS ultima_actualizacion
   FROM public.ciclos c
  WHERE (id_estado_ciclo = 2)
  WITH NO DATA;


--
-- Name: niveles_academicos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.niveles_academicos (
    id_nivel bigint NOT NULL,
    nivel character varying(50) NOT NULL
);


--
-- Name: niveles_academicos_id_nivel_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.niveles_academicos ALTER COLUMN id_nivel ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.niveles_academicos_id_nivel_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pagos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagos (
    id_pago bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_metodo_pago bigint NOT NULL,
    id_canal_pago bigint NOT NULL,
    id_punto_recaudo bigint,
    id_usuario_registro bigint,
    id_estado_pago bigint NOT NULL,
    monto_pagado numeric(14,2) NOT NULL,
    fecha_pago timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    numero_autorizacion character varying(100),
    referencia character varying(100),
    CONSTRAINT chk_monto_pago CHECK ((monto_pagado > (0)::numeric))
);


--
-- Name: pagos_cargos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagos_cargos (
    id_pago_cargo bigint NOT NULL,
    id_pago bigint NOT NULL,
    id_cargo bigint NOT NULL,
    monto_aplicado numeric(14,2) NOT NULL,
    CONSTRAINT chk_monto_aplicado CHECK ((monto_aplicado > (0)::numeric))
);


--
-- Name: pagos_cargos_id_pago_cargo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pagos_cargos ALTER COLUMN id_pago_cargo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pagos_cargos_id_pago_cargo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pagos_id_pago_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pagos ALTER COLUMN id_pago ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pagos_id_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pensum; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pensum (
    id_pensum bigint NOT NULL,
    id_carrera bigint NOT NULL,
    id_estado_pensum bigint NOT NULL,
    version integer NOT NULL,
    anio_inicio integer NOT NULL,
    anio_fin integer,
    CONSTRAINT chk_pensum_anios CHECK (((anio_fin IS NULL) OR (anio_fin >= anio_inicio)))
);


--
-- Name: pensum_id_pensum_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pensum ALTER COLUMN id_pensum ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pensum_id_pensum_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pensum_materias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pensum_materias (
    id_pensum_materia bigint NOT NULL,
    id_pensum bigint NOT NULL,
    id_materia bigint NOT NULL,
    ciclo_recomendado integer NOT NULL,
    obligatoria boolean DEFAULT true NOT NULL,
    CONSTRAINT chk_ciclo_recomendado CHECK ((ciclo_recomendado > 0))
);


--
-- Name: pensum_materias_id_pensum_materia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pensum_materias ALTER COLUMN id_pensum_materia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pensum_materias_id_pensum_materia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: periodos_academicos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.periodos_academicos (
    id_periodo bigint NOT NULL,
    id_ciclo bigint NOT NULL,
    id_tipo_periodo bigint NOT NULL,
    fecha_inicio timestamp with time zone NOT NULL,
    fecha_fin timestamp with time zone NOT NULL,
    CONSTRAINT chk_periodo_fechas CHECK ((fecha_fin > fecha_inicio))
);


--
-- Name: periodos_academicos_id_periodo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.periodos_academicos ALTER COLUMN id_periodo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.periodos_academicos_id_periodo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: periodos_evaluacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.periodos_evaluacion (
    id_periodo_evaluacion bigint NOT NULL,
    id_ciclo bigint NOT NULL,
    periodo integer NOT NULL,
    porcentaje numeric(5,2) NOT NULL,
    CONSTRAINT periodos_evaluacion_periodo_check CHECK ((periodo > 0)),
    CONSTRAINT periodos_evaluacion_porcentaje_check CHECK (((porcentaje > (0)::numeric) AND (porcentaje <= (100)::numeric)))
);


--
-- Name: periodos_evaluacion_id_periodo_evaluacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.periodos_evaluacion ALTER COLUMN id_periodo_evaluacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.periodos_evaluacion_id_periodo_evaluacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: periodos_tipos_evaluacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.periodos_tipos_evaluacion (
    id_periodo_tipo_evaluacion bigint NOT NULL,
    id_periodo_evaluacion bigint NOT NULL,
    id_tipo_evaluacion bigint NOT NULL,
    porcentaje numeric NOT NULL,
    CONSTRAINT periodos_tipos_evaluacion_porcentaje_check CHECK (((porcentaje > (0)::numeric) AND (porcentaje <= (100)::numeric)))
);


--
-- Name: periodos_tipos_evaluacion_id_periodo_tipo_evaluacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.periodos_tipos_evaluacion ALTER COLUMN id_periodo_tipo_evaluacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.periodos_tipos_evaluacion_id_periodo_tipo_evaluacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: personas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personas (
    id_persona bigint NOT NULL,
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    fecha_nacimiento date NOT NULL,
    telefono character varying(25),
    correo_personal character varying(150),
    direccion text,
    id_estado_registro bigint NOT NULL,
    fecha_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    id_tipo_documento bigint NOT NULL,
    numero_documento character varying(20) NOT NULL,
    sexo character varying(1) DEFAULT 'M'::character varying NOT NULL,
    id_distrito bigint DEFAULT 22 NOT NULL,
    url_foto character varying(500),
    CONSTRAINT chk_personas_documento_longitud CHECK (((char_length(TRIM(BOTH FROM numero_documento)) >= 7) AND (char_length(TRIM(BOTH FROM numero_documento)) <= 20))),
    CONSTRAINT chk_personas_sexo CHECK (((sexo)::text = ANY (ARRAY[('M'::character varying)::text, ('F'::character varying)::text])))
);


--
-- Name: personas_id_persona_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.personas ALTER COLUMN id_persona ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.personas_id_persona_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: plantillas_horarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.plantillas_horarios (
    id_plantilla bigint NOT NULL,
    codigo_plantilla character varying NOT NULL,
    descripcion character varying,
    activa boolean DEFAULT true NOT NULL
);


--
-- Name: plantillas_horarios_detalle; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.plantillas_horarios_detalle (
    id_plantilla_detalle bigint NOT NULL,
    id_plantilla bigint NOT NULL,
    id_dia bigint NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    CONSTRAINT chk_plantilla_rango_horas CHECK ((hora_inicio < hora_fin))
);


--
-- Name: plantillas_horarios_detalle_id_plantilla_detalle_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.plantillas_horarios_detalle ALTER COLUMN id_plantilla_detalle ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.plantillas_horarios_detalle_id_plantilla_detalle_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: plantillas_horarios_id_plantilla_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.plantillas_horarios ALTER COLUMN id_plantilla ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.plantillas_horarios_id_plantilla_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: puntos_recaudo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.puntos_recaudo (
    id_punto_recaudo bigint NOT NULL,
    id_sede bigint NOT NULL,
    punto_recaudo character varying(150) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: puntos_recaudo_id_punto_recaudo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.puntos_recaudo ALTER COLUMN id_punto_recaudo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.puntos_recaudo_id_punto_recaudo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: recargos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recargos (
    id_recargo bigint NOT NULL,
    id_tipo_cobro bigint NOT NULL,
    id_estado_recargo bigint NOT NULL,
    recargo numeric(14,2) NOT NULL,
    dias_gracia integer DEFAULT 0 NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date,
    CONSTRAINT chk_dias_gracia CHECK ((dias_gracia >= 0)),
    CONSTRAINT chk_fechas_recargo CHECK (((fecha_fin IS NULL) OR (fecha_fin >= fecha_inicio))),
    CONSTRAINT chk_recargo CHECK ((recargo > (0)::numeric))
);


--
-- Name: recargos_id_recargo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.recargos ALTER COLUMN id_recargo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.recargos_id_recargo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: recuperaciones_clave; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recuperaciones_clave (
    id_recuperacion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    codigo_hash character varying(255) NOT NULL,
    fecha_generacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_expiracion timestamp with time zone NOT NULL,
    fecha_uso timestamp with time zone,
    usado boolean DEFAULT false NOT NULL
);


--
-- Name: recuperaciones_clave_id_recuperacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.recuperaciones_clave ALTER COLUMN id_recuperacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.recuperaciones_clave_id_recuperacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id_rol bigint NOT NULL,
    rol character varying(80) NOT NULL
);


--
-- Name: roles_id_rol_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.roles ALTER COLUMN id_rol ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.roles_id_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sedes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sedes (
    id_sede bigint NOT NULL,
    id_tipo_sede bigint NOT NULL,
    id_estado_sede bigint NOT NULL,
    codigo_sede character varying(255) NOT NULL,
    nombre_sede character varying(255) NOT NULL,
    direccion character varying(255),
    telefono character varying(255),
    correo character varying(255)
);


--
-- Name: sedes_id_sede_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.sedes ALTER COLUMN id_sede ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.sedes_id_sede_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sesiones_usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sesiones_usuario (
    id_sesion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_rol bigint NOT NULL,
    fecha_inicio timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_fin timestamp with time zone,
    direccion_ip character varying(45),
    agente_usuario text,
    exitosa boolean DEFAULT true NOT NULL
);


--
-- Name: sesiones_usuario_id_sesion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.sesiones_usuario ALTER COLUMN id_sesion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.sesiones_usuario_id_sesion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: solicitudes_calificacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.solicitudes_calificacion (
    id_solicitud bigint NOT NULL,
    id_calificacion bigint NOT NULL,
    id_usuario_solicitante bigint NOT NULL,
    id_usuario_autorizador bigint,
    id_estado_solicitud bigint NOT NULL,
    motivo text NOT NULL,
    respuesta text,
    fecha_solicitud timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_respuesta timestamp with time zone,
    CONSTRAINT chk_solicitud_fechas CHECK (((fecha_respuesta IS NULL) OR (fecha_respuesta >= fecha_solicitud)))
);


--
-- Name: solicitudes_calificacion_id_solicitud_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.solicitudes_calificacion ALTER COLUMN id_solicitud ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.solicitudes_calificacion_id_solicitud_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_aula; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_aula (
    id_tipo_aula bigint NOT NULL,
    tipo_aula character varying(50) NOT NULL
);


--
-- Name: tipos_aula_id_tipo_aula_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_aula ALTER COLUMN id_tipo_aula ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_aula_id_tipo_aula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_cobro; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_cobro (
    id_tipo_cobro bigint NOT NULL,
    tipo_cobro character varying(200) NOT NULL
);


--
-- Name: tipos_cobro_id_tipo_cobro_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_cobro ALTER COLUMN id_tipo_cobro ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_cobro_id_tipo_cobro_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_contratacion_docente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_contratacion_docente (
    id_tipo_contratacion bigint NOT NULL,
    tipo_contratacion character varying(255) NOT NULL,
    maximo_materias integer NOT NULL,
    CONSTRAINT chk_maximo_materias CHECK ((maximo_materias > 0))
);


--
-- Name: tipos_contratacion_docente_id_tipo_contratacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_contratacion_docente ALTER COLUMN id_tipo_contratacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_contratacion_docente_id_tipo_contratacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_documento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_documento (
    id_tipo_documento bigint NOT NULL,
    tipo_documento character varying(80) NOT NULL
);


--
-- Name: tipos_documento_id_tipo_documento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_documento ALTER COLUMN id_tipo_documento ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_documento_id_tipo_documento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_evaluacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_evaluacion (
    id_tipo_evaluacion bigint NOT NULL,
    tipo_evaluacion character varying(50) NOT NULL
);


--
-- Name: tipos_evaluacion_id_tipo_evaluacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_evaluacion ALTER COLUMN id_tipo_evaluacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_evaluacion_id_tipo_evaluacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_periodo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_periodo (
    id_tipo_periodo bigint NOT NULL,
    tipo_periodo character varying(50) NOT NULL
);


--
-- Name: tipos_periodo_id_tipo_periodo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_periodo ALTER COLUMN id_tipo_periodo ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_periodo_id_tipo_periodo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_sede; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_sede (
    id_tipo_sede bigint NOT NULL,
    tipo_sede character varying(255) NOT NULL
);


--
-- Name: tipos_sede_id_tipo_sede_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_sede ALTER COLUMN id_tipo_sede ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tipos_sede_id_tipo_sede_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id_usuario bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_rol bigint NOT NULL,
    id_estado_usuario bigint NOT NULL,
    correo_institucional character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ultimo_acceso timestamp with time zone,
    bloqueado_hasta timestamp with time zone,
    intentos_fallidos integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_intentos_fallidos CHECK ((intentos_fallidos >= 0))
);


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id_usuario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vw_dashboard_gerencial; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_dashboard_gerencial AS
 WITH matriculas_por_sede AS (
         SELECT m.id_ciclo,
            cs.id_sede,
            count(DISTINCT m.id_matricula) AS total_matricula
           FROM ((public.matriculas m
             JOIN public.estudiantes_carreras ec ON ((m.id_estudiante_carrera = ec.id_estudiante_carrera)))
             JOIN public.carreras_sedes cs ON ((ec.id_carrera_sede = cs.id_carrera_sede)))
          WHERE (m.id_estado_matricula = 2)
          GROUP BY m.id_ciclo, cs.id_sede
        ), inscripciones_por_grupo AS (
         SELECT i.id_grupo,
            count(*) AS total_inscripciones
           FROM public.inscripciones i
          WHERE (i.id_estado_inscripcion = 1)
          GROUP BY i.id_grupo
        ), grupos_por_sede AS (
         SELECT g.id_ciclo,
            g.id_sede,
            count(g.id_grupo) AS total_secciones,
            count(DISTINCT g.id_docente) AS total_docentes,
            COALESCE(round(avg((((COALESCE(ipg.total_inscripciones, (0)::bigint))::numeric / (NULLIF(g.cupo_maximo, 0))::numeric) * (100)::numeric)), 2), (0)::numeric) AS capacidad_promedio,
            COALESCE(round(((sum(COALESCE(ipg.total_inscripciones, (0)::bigint)) / (NULLIF(sum(g.cupo_maximo), 0))::numeric) * (100)::numeric), 2), (0)::numeric) AS ocupacion_global
           FROM (public.grupos g
             LEFT JOIN inscripciones_por_grupo ipg ON ((g.id_grupo = ipg.id_grupo)))
          WHERE (g.id_estado_grupo = 1)
          GROUP BY g.id_ciclo, g.id_sede
        ), facturacion_por_sede AS (
         SELECT m.id_ciclo,
            cs.id_sede,
            COALESCE(sum(ce.monto_total), (0)::numeric) AS total_facturado
           FROM (((public.cargos_estudiante ce
             JOIN public.matriculas m ON ((ce.id_matricula = m.id_matricula)))
             JOIN public.estudiantes_carreras ec ON ((m.id_estudiante_carrera = ec.id_estudiante_carrera)))
             JOIN public.carreras_sedes cs ON ((ec.id_carrera_sede = cs.id_carrera_sede)))
          WHERE (ce.id_estado_cargo <> 3)
          GROUP BY m.id_ciclo, cs.id_sede
        ), recaudacion_por_sede AS (
         SELECT m.id_ciclo,
            cs.id_sede,
            COALESCE(sum(pc.monto_aplicado), (0)::numeric) AS total_recaudado
           FROM (((((public.pagos_cargos pc
             JOIN public.pagos p ON ((pc.id_pago = p.id_pago)))
             JOIN public.cargos_estudiante ce ON ((pc.id_cargo = ce.id_cargo_estudiante)))
             JOIN public.matriculas m ON ((ce.id_matricula = m.id_matricula)))
             JOIN public.estudiantes_carreras ec ON ((m.id_estudiante_carrera = ec.id_estudiante_carrera)))
             JOIN public.carreras_sedes cs ON ((ec.id_carrera_sede = cs.id_carrera_sede)))
          WHERE ((p.id_estado_pago = 2) AND (ce.id_estado_cargo <> 3))
          GROUP BY m.id_ciclo, cs.id_sede
        )
 SELECT c.id_ciclo,
    c.codigo_ciclo,
    c.anio,
    c.numero_ciclo,
    s.id_sede,
    s.nombre_sede,
    COALESCE(mp.total_matricula, (0)::bigint) AS total_matricula,
    COALESCE(gp.total_docentes, (0)::bigint) AS total_docentes,
    COALESCE(gp.total_secciones, (0)::bigint) AS total_secciones,
    COALESCE(gp.capacidad_promedio, (0)::numeric) AS capacidad_promedio,
    COALESCE(gp.ocupacion_global, (0)::numeric) AS ocupacion_global,
    COALESCE(fp.total_facturado, (0)::numeric) AS total_facturado,
    COALESCE(rp.total_recaudado, (0)::numeric) AS total_recaudado
   FROM (((((public.ciclos c
     CROSS JOIN public.sedes s)
     LEFT JOIN matriculas_por_sede mp ON (((mp.id_ciclo = c.id_ciclo) AND (mp.id_sede = s.id_sede))))
     LEFT JOIN grupos_por_sede gp ON (((gp.id_ciclo = c.id_ciclo) AND (gp.id_sede = s.id_sede))))
     LEFT JOIN facturacion_por_sede fp ON (((fp.id_ciclo = c.id_ciclo) AND (fp.id_sede = s.id_sede))))
     LEFT JOIN recaudacion_por_sede rp ON (((rp.id_ciclo = c.id_ciclo) AND (rp.id_sede = s.id_sede))));


--
-- Name: departamentos id_departamento; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departamentos ALTER COLUMN id_departamento SET DEFAULT nextval('public.departamentos_id_departamento_seq'::regclass);


--
-- Name: distritos id_distrito; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distritos ALTER COLUMN id_distrito SET DEFAULT nextval('public.distritos_id_distrito_seq'::regclass);


--
-- Name: areas areas_area_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas
    ADD CONSTRAINT areas_area_key UNIQUE (area);


--
-- Name: areas areas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas
    ADD CONSTRAINT areas_pkey PRIMARY KEY (id_area);


--
-- Name: asistencias asistencias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT asistencias_pkey PRIMARY KEY (id_asistencia);


--
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id_auditoria);


--
-- Name: aulas aulas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT aulas_pkey PRIMARY KEY (id_aula);


--
-- Name: beneficios beneficios_beneficio_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios
    ADD CONSTRAINT beneficios_beneficio_key UNIQUE (beneficio);


--
-- Name: beneficios_cobro beneficios_cobro_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios_cobro
    ADD CONSTRAINT beneficios_cobro_pkey PRIMARY KEY (id_beneficio_cobro);


--
-- Name: beneficios beneficios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios
    ADD CONSTRAINT beneficios_pkey PRIMARY KEY (id_beneficio);


--
-- Name: calificaciones calificaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT calificaciones_pkey PRIMARY KEY (id_calificacion);


--
-- Name: canales_pago canales_pago_canal_pago_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.canales_pago
    ADD CONSTRAINT canales_pago_canal_pago_key UNIQUE (canal_pago);


--
-- Name: canales_pago canales_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.canales_pago
    ADD CONSTRAINT canales_pago_pkey PRIMARY KEY (id_canal_pago);


--
-- Name: cargos cargos_cargo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos
    ADD CONSTRAINT cargos_cargo_key UNIQUE (cargo);


--
-- Name: cargos_estudiante cargos_estudiante_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos_estudiante
    ADD CONSTRAINT cargos_estudiante_pkey PRIMARY KEY (id_cargo_estudiante);


--
-- Name: cargos cargos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos
    ADD CONSTRAINT cargos_pkey PRIMARY KEY (id_cargo);


--
-- Name: carreras carreras_codigo_carrera_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras
    ADD CONSTRAINT carreras_codigo_carrera_key UNIQUE (codigo_carrera);


--
-- Name: carreras carreras_nombre_carrera_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras
    ADD CONSTRAINT carreras_nombre_carrera_key UNIQUE (nombre_carrera);


--
-- Name: carreras carreras_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras
    ADD CONSTRAINT carreras_pkey PRIMARY KEY (id_carrera);


--
-- Name: carreras_sedes carreras_sedes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras_sedes
    ADD CONSTRAINT carreras_sedes_pkey PRIMARY KEY (id_carrera_sede);


--
-- Name: ciclos ciclos_codigo_ciclo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ciclos
    ADD CONSTRAINT ciclos_codigo_ciclo_key UNIQUE (codigo_ciclo);


--
-- Name: ciclos ciclos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ciclos
    ADD CONSTRAINT ciclos_pkey PRIMARY KEY (id_ciclo);


--
-- Name: clases clases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clases
    ADD CONSTRAINT clases_pkey PRIMARY KEY (id_clase);


--
-- Name: conceptos_cobro conceptos_cobro_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_pkey PRIMARY KEY (id_concepto_cobro);


--
-- Name: departamentos departamentos_nombre_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departamentos
    ADD CONSTRAINT departamentos_nombre_key UNIQUE (nombre);


--
-- Name: departamentos departamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departamentos
    ADD CONSTRAINT departamentos_pkey PRIMARY KEY (id_departamento);


--
-- Name: dias_semana dias_semana_dia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dias_semana
    ADD CONSTRAINT dias_semana_dia_key UNIQUE (dia);


--
-- Name: dias_semana dias_semana_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dias_semana
    ADD CONSTRAINT dias_semana_pkey PRIMARY KEY (id_dia);


--
-- Name: distritos distritos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distritos
    ADD CONSTRAINT distritos_pkey PRIMARY KEY (id_distrito);


--
-- Name: docentes docentes_codigo_docente_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT docentes_codigo_docente_key UNIQUE (codigo_docente);


--
-- Name: docentes docentes_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT docentes_id_usuario_key UNIQUE (id_usuario);


--
-- Name: docentes docentes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT docentes_pkey PRIMARY KEY (id_docente);


--
-- Name: edificios edificios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.edificios
    ADD CONSTRAINT edificios_pkey PRIMARY KEY (id_edificio);


--
-- Name: empleados empleados_codigo_empleado_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT empleados_codigo_empleado_key UNIQUE (codigo_empleado);


--
-- Name: empleados empleados_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT empleados_id_usuario_key UNIQUE (id_usuario);


--
-- Name: empleados empleados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT empleados_pkey PRIMARY KEY (id_empleado);


--
-- Name: estados_asistencia estados_asistencia_estado_asistencia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_asistencia
    ADD CONSTRAINT estados_asistencia_estado_asistencia_key UNIQUE (estado_asistencia);


--
-- Name: estados_asistencia estados_asistencia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_asistencia
    ADD CONSTRAINT estados_asistencia_pkey PRIMARY KEY (id_estado_asistencia);


--
-- Name: estados_aula estados_aula_estado_aula_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_aula
    ADD CONSTRAINT estados_aula_estado_aula_key UNIQUE (estado_aula);


--
-- Name: estados_aula estados_aula_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_aula
    ADD CONSTRAINT estados_aula_pkey PRIMARY KEY (id_estado_aula);


--
-- Name: estados_beneficio estados_beneficio_estado_beneficio_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_beneficio
    ADD CONSTRAINT estados_beneficio_estado_beneficio_key UNIQUE (estado_beneficio);


--
-- Name: estados_beneficio_estudiante estados_beneficio_estudiante_estado_beneficio_estudiante_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_beneficio_estudiante
    ADD CONSTRAINT estados_beneficio_estudiante_estado_beneficio_estudiante_key UNIQUE (estado_beneficio_estudiante);


--
-- Name: estados_beneficio_estudiante estados_beneficio_estudiante_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_beneficio_estudiante
    ADD CONSTRAINT estados_beneficio_estudiante_pkey PRIMARY KEY (id_estado_beneficio_estudiante);


--
-- Name: estados_beneficio estados_beneficio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_beneficio
    ADD CONSTRAINT estados_beneficio_pkey PRIMARY KEY (id_estado_beneficio);


--
-- Name: estados_calificacion estados_calificacion_estado_calificacion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_calificacion
    ADD CONSTRAINT estados_calificacion_estado_calificacion_key UNIQUE (estado_calificacion);


--
-- Name: estados_calificacion estados_calificacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_calificacion
    ADD CONSTRAINT estados_calificacion_pkey PRIMARY KEY (id_estado_calificacion);


--
-- Name: estados_cargo estados_cargo_estado_cargo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_cargo
    ADD CONSTRAINT estados_cargo_estado_cargo_key UNIQUE (estado_cargo);


--
-- Name: estados_cargo estados_cargo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_cargo
    ADD CONSTRAINT estados_cargo_pkey PRIMARY KEY (id_estado_cargo);


--
-- Name: estados_carrera estados_carrera_estado_carrera_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_carrera
    ADD CONSTRAINT estados_carrera_estado_carrera_key UNIQUE (estado_carrera);


--
-- Name: estados_carrera estados_carrera_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_carrera
    ADD CONSTRAINT estados_carrera_pkey PRIMARY KEY (id_estado_carrera);


--
-- Name: estados_ciclo estados_ciclo_estado_ciclo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_ciclo
    ADD CONSTRAINT estados_ciclo_estado_ciclo_key UNIQUE (estado_ciclo);


--
-- Name: estados_ciclo estados_ciclo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_ciclo
    ADD CONSTRAINT estados_ciclo_pkey PRIMARY KEY (id_estado_ciclo);


--
-- Name: estados_docente estados_docente_estado_docente_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_docente
    ADD CONSTRAINT estados_docente_estado_docente_key UNIQUE (estado_docente);


--
-- Name: estados_docente estados_docente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_docente
    ADD CONSTRAINT estados_docente_pkey PRIMARY KEY (id_estado_docente);


--
-- Name: estados_edificio estados_edificio_estado_edificio_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_edificio
    ADD CONSTRAINT estados_edificio_estado_edificio_key UNIQUE (estado_edificio);


--
-- Name: estados_edificio estados_edificio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_edificio
    ADD CONSTRAINT estados_edificio_pkey PRIMARY KEY (id_estado_edificio);


--
-- Name: estados_empleado estados_empleado_estado_empleado_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_empleado
    ADD CONSTRAINT estados_empleado_estado_empleado_key UNIQUE (estado_empleado);


--
-- Name: estados_empleado estados_empleado_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_empleado
    ADD CONSTRAINT estados_empleado_pkey PRIMARY KEY (id_estado_empleado);


--
-- Name: estados_estudiante estados_estudiante_estado_estudiante_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_estudiante
    ADD CONSTRAINT estados_estudiante_estado_estudiante_key UNIQUE (estado_estudiante);


--
-- Name: estados_estudiante estados_estudiante_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_estudiante
    ADD CONSTRAINT estados_estudiante_pkey PRIMARY KEY (id_estado_estudiante);


--
-- Name: estados_factura estados_factura_estado_factura_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_factura
    ADD CONSTRAINT estados_factura_estado_factura_key UNIQUE (estado_factura);


--
-- Name: estados_factura estados_factura_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_factura
    ADD CONSTRAINT estados_factura_pkey PRIMARY KEY (id_estado_factura);


--
-- Name: estados_facultad estados_facultad_estado_facultad_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_facultad
    ADD CONSTRAINT estados_facultad_estado_facultad_key UNIQUE (estado_facultad);


--
-- Name: estados_facultad estados_facultad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_facultad
    ADD CONSTRAINT estados_facultad_pkey PRIMARY KEY (id_estado_facultad);


--
-- Name: estados_grupo estados_grupo_estado_grupo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_grupo
    ADD CONSTRAINT estados_grupo_estado_grupo_key UNIQUE (estado_grupo);


--
-- Name: estados_grupo estados_grupo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_grupo
    ADD CONSTRAINT estados_grupo_pkey PRIMARY KEY (id_estado_grupo);


--
-- Name: estados_inscripcion estados_inscripcion_estado_inscripcion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_inscripcion
    ADD CONSTRAINT estados_inscripcion_estado_inscripcion_key UNIQUE (estado_inscripcion);


--
-- Name: estados_inscripcion estados_inscripcion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_inscripcion
    ADD CONSTRAINT estados_inscripcion_pkey PRIMARY KEY (id_estado_inscripcion);


--
-- Name: estados_materia_estudiante estados_materia_estudiante_estado_materia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_materia_estudiante
    ADD CONSTRAINT estados_materia_estudiante_estado_materia_key UNIQUE (estado_materia);


--
-- Name: estados_materia_estudiante estados_materia_estudiante_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_materia_estudiante
    ADD CONSTRAINT estados_materia_estudiante_pkey PRIMARY KEY (id_estado_materia);


--
-- Name: estados_matricula estados_matricula_estado_matricula_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_matricula
    ADD CONSTRAINT estados_matricula_estado_matricula_key UNIQUE (estado_matricula);


--
-- Name: estados_matricula estados_matricula_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_matricula
    ADD CONSTRAINT estados_matricula_pkey PRIMARY KEY (id_estado_matricula);


--
-- Name: estados_pago estados_pago_estado_pago_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_pago
    ADD CONSTRAINT estados_pago_estado_pago_key UNIQUE (estado_pago);


--
-- Name: estados_pago estados_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_pago
    ADD CONSTRAINT estados_pago_pkey PRIMARY KEY (id_estado_pago);


--
-- Name: estados_pensum estados_pensum_estado_pensum_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_pensum
    ADD CONSTRAINT estados_pensum_estado_pensum_key UNIQUE (estado_pensum);


--
-- Name: estados_pensum estados_pensum_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_pensum
    ADD CONSTRAINT estados_pensum_pkey PRIMARY KEY (id_estado_pensum);


--
-- Name: estados_recargo estados_recargo_estado_recargo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_recargo
    ADD CONSTRAINT estados_recargo_estado_recargo_key UNIQUE (estado_recargo);


--
-- Name: estados_recargo estados_recargo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_recargo
    ADD CONSTRAINT estados_recargo_pkey PRIMARY KEY (id_estado_recargo);


--
-- Name: estados_registro_persona estados_registro_persona_estado_registro_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_registro_persona
    ADD CONSTRAINT estados_registro_persona_estado_registro_key UNIQUE (estado_registro);


--
-- Name: estados_registro_persona estados_registro_persona_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_registro_persona
    ADD CONSTRAINT estados_registro_persona_pkey PRIMARY KEY (id_estado_registro);


--
-- Name: estados_sede estados_sede_estado_sede_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_sede
    ADD CONSTRAINT estados_sede_estado_sede_key UNIQUE (estado_sede);


--
-- Name: estados_sede estados_sede_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_sede
    ADD CONSTRAINT estados_sede_pkey PRIMARY KEY (id_estado_sede);


--
-- Name: estados_solicitud estados_solicitud_estado_solicitud_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_solicitud
    ADD CONSTRAINT estados_solicitud_estado_solicitud_key UNIQUE (estado_solicitud);


--
-- Name: estados_solicitud estados_solicitud_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_solicitud
    ADD CONSTRAINT estados_solicitud_pkey PRIMARY KEY (id_estado_solicitud);


--
-- Name: estados_trayectoria estados_trayectoria_estado_trayectoria_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_trayectoria
    ADD CONSTRAINT estados_trayectoria_estado_trayectoria_key UNIQUE (estado_trayectoria);


--
-- Name: estados_trayectoria estados_trayectoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_trayectoria
    ADD CONSTRAINT estados_trayectoria_pkey PRIMARY KEY (id_estado_trayectoria);


--
-- Name: estados_usuario estados_usuario_estado_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_usuario
    ADD CONSTRAINT estados_usuario_estado_usuario_key UNIQUE (estado_usuario);


--
-- Name: estados_usuario estados_usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estados_usuario
    ADD CONSTRAINT estados_usuario_pkey PRIMARY KEY (id_estado_usuario);


--
-- Name: estudiantes_beneficios estudiantes_beneficios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_beneficios
    ADD CONSTRAINT estudiantes_beneficios_pkey PRIMARY KEY (id_estudiante_beneficio);


--
-- Name: estudiantes estudiantes_carnet_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_carnet_key UNIQUE (carnet);


--
-- Name: estudiantes_carreras estudiantes_carreras_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_carreras
    ADD CONSTRAINT estudiantes_carreras_pkey PRIMARY KEY (id_estudiante_carrera);


--
-- Name: estudiantes estudiantes_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_id_usuario_key UNIQUE (id_usuario);


--
-- Name: estudiantes estudiantes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_pkey PRIMARY KEY (id_estudiante);


--
-- Name: evaluaciones evaluaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT evaluaciones_pkey PRIMARY KEY (id_evaluacion);


--
-- Name: facturas facturas_id_pago_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facturas
    ADD CONSTRAINT facturas_id_pago_key UNIQUE (id_pago);


--
-- Name: facturas facturas_numero_factura_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facturas
    ADD CONSTRAINT facturas_numero_factura_key UNIQUE (numero_factura);


--
-- Name: facturas facturas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facturas
    ADD CONSTRAINT facturas_pkey PRIMARY KEY (id_factura);


--
-- Name: facultades facultades_codigo_facultad_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades
    ADD CONSTRAINT facultades_codigo_facultad_key UNIQUE (codigo_facultad);


--
-- Name: facultades facultades_nombre_facultad_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades
    ADD CONSTRAINT facultades_nombre_facultad_key UNIQUE (nombre_facultad);


--
-- Name: facultades facultades_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades
    ADD CONSTRAINT facultades_pkey PRIMARY KEY (id_facultad);


--
-- Name: facultades_sedes facultades_sedes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades_sedes
    ADD CONSTRAINT facultades_sedes_pkey PRIMARY KEY (id_facultad_sede);


--
-- Name: grupos grupos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT grupos_pkey PRIMARY KEY (id_grupo);


--
-- Name: horarios horarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT horarios_pkey PRIMARY KEY (id_horario);


--
-- Name: inscripciones inscripciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT inscripciones_pkey PRIMARY KEY (id_inscripcion);


--
-- Name: intentos_acceso intentos_acceso_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intentos_acceso
    ADD CONSTRAINT intentos_acceso_pkey PRIMARY KEY (id_intento);


--
-- Name: materias materias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT materias_pkey PRIMARY KEY (id_materia);


--
-- Name: materias_prerrequisitos materias_prerrequisitos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias_prerrequisitos
    ADD CONSTRAINT materias_prerrequisitos_pkey PRIMARY KEY (id_prerrequisito);


--
-- Name: matriculas matriculas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_pkey PRIMARY KEY (id_matricula);


--
-- Name: metodos_pago metodos_pago_metodo_pago_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.metodos_pago
    ADD CONSTRAINT metodos_pago_metodo_pago_key UNIQUE (metodo_pago);


--
-- Name: metodos_pago metodos_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.metodos_pago
    ADD CONSTRAINT metodos_pago_pkey PRIMARY KEY (id_metodo_pago);


--
-- Name: modalidades modalidades_modalidad_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modalidades
    ADD CONSTRAINT modalidades_modalidad_key UNIQUE (modalidad);


--
-- Name: modalidades modalidades_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modalidades
    ADD CONSTRAINT modalidades_pkey PRIMARY KEY (id_modalidad);


--
-- Name: niveles_academicos niveles_academicos_nivel_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveles_academicos
    ADD CONSTRAINT niveles_academicos_nivel_key UNIQUE (nivel);


--
-- Name: niveles_academicos niveles_academicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveles_academicos
    ADD CONSTRAINT niveles_academicos_pkey PRIMARY KEY (id_nivel);


--
-- Name: pagos_cargos pagos_cargos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_cargos
    ADD CONSTRAINT pagos_cargos_pkey PRIMARY KEY (id_pago_cargo);


--
-- Name: pagos pagos_numero_autorizacion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_numero_autorizacion_key UNIQUE (numero_autorizacion);


--
-- Name: pagos pagos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_pkey PRIMARY KEY (id_pago);


--
-- Name: pagos pagos_referencia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_referencia_key UNIQUE (referencia);


--
-- Name: pensum_materias pensum_materias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum_materias
    ADD CONSTRAINT pensum_materias_pkey PRIMARY KEY (id_pensum_materia);


--
-- Name: pensum pensum_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum
    ADD CONSTRAINT pensum_pkey PRIMARY KEY (id_pensum);


--
-- Name: periodos_academicos periodos_academicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_pkey PRIMARY KEY (id_periodo);


--
-- Name: periodos_evaluacion periodos_evaluacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_evaluacion
    ADD CONSTRAINT periodos_evaluacion_pkey PRIMARY KEY (id_periodo_evaluacion);


--
-- Name: periodos_tipos_evaluacion periodos_tipos_evaluacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_tipos_evaluacion
    ADD CONSTRAINT periodos_tipos_evaluacion_pkey PRIMARY KEY (id_periodo_tipo_evaluacion);


--
-- Name: personas personas_correo_personal_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT personas_correo_personal_key UNIQUE (correo_personal);


--
-- Name: personas personas_numero_documento_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT personas_numero_documento_key UNIQUE (numero_documento);


--
-- Name: personas personas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT personas_pkey PRIMARY KEY (id_persona);


--
-- Name: plantillas_horarios plantillas_horarios_codigo_plantilla_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plantillas_horarios
    ADD CONSTRAINT plantillas_horarios_codigo_plantilla_key UNIQUE (codigo_plantilla);


--
-- Name: plantillas_horarios_detalle plantillas_horarios_detalle_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plantillas_horarios_detalle
    ADD CONSTRAINT plantillas_horarios_detalle_pkey PRIMARY KEY (id_plantilla_detalle);


--
-- Name: plantillas_horarios plantillas_horarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plantillas_horarios
    ADD CONSTRAINT plantillas_horarios_pkey PRIMARY KEY (id_plantilla);


--
-- Name: puntos_recaudo puntos_recaudo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.puntos_recaudo
    ADD CONSTRAINT puntos_recaudo_pkey PRIMARY KEY (id_punto_recaudo);


--
-- Name: recargos recargos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recargos
    ADD CONSTRAINT recargos_pkey PRIMARY KEY (id_recargo);


--
-- Name: recuperaciones_clave recuperaciones_clave_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recuperaciones_clave
    ADD CONSTRAINT recuperaciones_clave_pkey PRIMARY KEY (id_recuperacion);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id_rol);


--
-- Name: roles roles_rol_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_rol_key UNIQUE (rol);


--
-- Name: sedes sedes_codigo_sede_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sedes
    ADD CONSTRAINT sedes_codigo_sede_key UNIQUE (codigo_sede);


--
-- Name: sedes sedes_nombre_sede_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sedes
    ADD CONSTRAINT sedes_nombre_sede_key UNIQUE (nombre_sede);


--
-- Name: sedes sedes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sedes
    ADD CONSTRAINT sedes_pkey PRIMARY KEY (id_sede);


--
-- Name: sesiones_usuario sesiones_usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_usuario
    ADD CONSTRAINT sesiones_usuario_pkey PRIMARY KEY (id_sesion);


--
-- Name: solicitudes_calificacion solicitudes_calificacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_calificacion
    ADD CONSTRAINT solicitudes_calificacion_pkey PRIMARY KEY (id_solicitud);


--
-- Name: tipos_aula tipos_aula_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_aula
    ADD CONSTRAINT tipos_aula_pkey PRIMARY KEY (id_tipo_aula);


--
-- Name: tipos_aula tipos_aula_tipo_aula_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_aula
    ADD CONSTRAINT tipos_aula_tipo_aula_key UNIQUE (tipo_aula);


--
-- Name: tipos_cobro tipos_cobro_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_cobro
    ADD CONSTRAINT tipos_cobro_pkey PRIMARY KEY (id_tipo_cobro);


--
-- Name: tipos_cobro tipos_cobro_tipo_cobro_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_cobro
    ADD CONSTRAINT tipos_cobro_tipo_cobro_key UNIQUE (tipo_cobro);


--
-- Name: tipos_contratacion_docente tipos_contratacion_docente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_contratacion_docente
    ADD CONSTRAINT tipos_contratacion_docente_pkey PRIMARY KEY (id_tipo_contratacion);


--
-- Name: tipos_contratacion_docente tipos_contratacion_docente_tipo_contratacion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_contratacion_docente
    ADD CONSTRAINT tipos_contratacion_docente_tipo_contratacion_key UNIQUE (tipo_contratacion);


--
-- Name: tipos_documento tipos_documento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_documento
    ADD CONSTRAINT tipos_documento_pkey PRIMARY KEY (id_tipo_documento);


--
-- Name: tipos_documento tipos_documento_tipo_documento_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_documento
    ADD CONSTRAINT tipos_documento_tipo_documento_key UNIQUE (tipo_documento);


--
-- Name: tipos_evaluacion tipos_evaluacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_evaluacion
    ADD CONSTRAINT tipos_evaluacion_pkey PRIMARY KEY (id_tipo_evaluacion);


--
-- Name: tipos_evaluacion tipos_evaluacion_tipo_evaluacion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_evaluacion
    ADD CONSTRAINT tipos_evaluacion_tipo_evaluacion_key UNIQUE (tipo_evaluacion);


--
-- Name: tipos_periodo tipos_periodo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_periodo
    ADD CONSTRAINT tipos_periodo_pkey PRIMARY KEY (id_tipo_periodo);


--
-- Name: tipos_periodo tipos_periodo_tipo_periodo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_periodo
    ADD CONSTRAINT tipos_periodo_tipo_periodo_key UNIQUE (tipo_periodo);


--
-- Name: tipos_sede tipos_sede_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_sede
    ADD CONSTRAINT tipos_sede_pkey PRIMARY KEY (id_tipo_sede);


--
-- Name: tipos_sede tipos_sede_tipo_sede_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_sede
    ADD CONSTRAINT tipos_sede_tipo_sede_key UNIQUE (tipo_sede);


--
-- Name: carreras_sedes ukiey1785p7rt7bgbgrjoxae612; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras_sedes
    ADD CONSTRAINT ukiey1785p7rt7bgbgrjoxae612 UNIQUE (id_carrera, id_sede);


--
-- Name: pensum ukrrt034xvn8cvjstd0imdd0y7x; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum
    ADD CONSTRAINT ukrrt034xvn8cvjstd0imdd0y7x UNIQUE (id_carrera, version);


--
-- Name: calificaciones ukyp7s07kq49o7np74idcb9rbr; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT ukyp7s07kq49o7np74idcb9rbr UNIQUE (id_inscripcion, id_evaluacion);


--
-- Name: asistencias uq_asistencia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT uq_asistencia UNIQUE (id_clase, id_inscripcion);


--
-- Name: asistencias uq_asistencias; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT uq_asistencias UNIQUE (id_clase, id_inscripcion);


--
-- Name: aulas uq_aula_edificio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT uq_aula_edificio UNIQUE (id_edificio, codigo_aula);


--
-- Name: aulas uq_aulas_codigo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT uq_aulas_codigo UNIQUE (id_edificio, codigo_aula);


--
-- Name: aulas uq_aulas_codigo_global; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT uq_aulas_codigo_global UNIQUE (codigo_aula);


--
-- Name: beneficios_cobro uq_beneficio_cobro; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios_cobro
    ADD CONSTRAINT uq_beneficio_cobro UNIQUE (id_beneficio, id_tipo_cobro);


--
-- Name: beneficios_cobro uq_beneficios_cobro; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios_cobro
    ADD CONSTRAINT uq_beneficios_cobro UNIQUE (id_beneficio, id_tipo_cobro);


--
-- Name: calificaciones uq_calificacion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT uq_calificacion UNIQUE (id_inscripcion, id_evaluacion);


--
-- Name: calificaciones uq_calificaciones; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT uq_calificaciones UNIQUE (id_inscripcion, id_evaluacion);


--
-- Name: cargos_estudiante uq_cargo_cuota; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos_estudiante
    ADD CONSTRAINT uq_cargo_cuota UNIQUE (id_matricula, id_concepto_cobro, numero_cuota);


--
-- Name: carreras_sedes uq_carrera_sede; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras_sedes
    ADD CONSTRAINT uq_carrera_sede UNIQUE (id_carrera, id_sede);


--
-- Name: carreras_sedes uq_carreras_sedes; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras_sedes
    ADD CONSTRAINT uq_carreras_sedes UNIQUE (id_carrera, id_sede);


--
-- Name: ciclos uq_ciclo_anio_numero; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ciclos
    ADD CONSTRAINT uq_ciclo_anio_numero UNIQUE (anio, numero_ciclo);


--
-- Name: ciclos uq_ciclos_anio_numero; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ciclos
    ADD CONSTRAINT uq_ciclos_anio_numero UNIQUE (anio, numero_ciclo);


--
-- Name: clases uq_clase; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clases
    ADD CONSTRAINT uq_clase UNIQUE (id_grupo, fecha_clase);


--
-- Name: clases uq_clases; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clases
    ADD CONSTRAINT uq_clases UNIQUE (id_grupo, fecha_clase);


--
-- Name: conceptos_cobro uq_concepto_cobro; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT uq_concepto_cobro UNIQUE (id_ciclo, id_tipo_cobro);


--
-- Name: conceptos_cobro uq_conceptos_cobro; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT uq_conceptos_cobro UNIQUE (id_ciclo, id_tipo_cobro);


--
-- Name: edificios uq_edificio_sede; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.edificios
    ADD CONSTRAINT uq_edificio_sede UNIQUE (id_sede, codigo_edificio);


--
-- Name: edificios uq_edificios_codigo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.edificios
    ADD CONSTRAINT uq_edificios_codigo UNIQUE (codigo_edificio);


--
-- Name: estudiantes_beneficios uq_est_beneficio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_beneficios
    ADD CONSTRAINT uq_est_beneficio UNIQUE (id_estudiante, id_beneficio);


--
-- Name: estudiantes_carreras uq_estudiantes_carreras; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_carreras
    ADD CONSTRAINT uq_estudiantes_carreras UNIQUE (id_estudiante, id_carrera_sede, id_pensum);


--
-- Name: evaluaciones uq_evaluacion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT uq_evaluacion UNIQUE (id_grupo, id_tipo_evaluacion, numero_evaluacion);


--
-- Name: evaluaciones uq_evaluaciones; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT uq_evaluaciones UNIQUE (id_grupo, id_tipo_evaluacion, numero_evaluacion, id_periodo_evaluacion);


--
-- Name: facultades_sedes uq_facultad_sede; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades_sedes
    ADD CONSTRAINT uq_facultad_sede UNIQUE (id_facultad, id_sede);


--
-- Name: grupos uq_grupos_ciclo_materia_sede_codigo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT uq_grupos_ciclo_materia_sede_codigo UNIQUE (id_ciclo, id_materia, id_sede, codigo_grupo);


--
-- Name: horarios uq_horario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT uq_horario UNIQUE (id_grupo, id_dia, hora_inicio, hora_fin);


--
-- Name: horarios uq_horarios; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT uq_horarios UNIQUE (id_grupo, id_dia, hora_inicio);


--
-- Name: inscripciones uq_inscripcion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT uq_inscripcion UNIQUE (id_matricula, id_grupo);


--
-- Name: inscripciones uq_inscripciones; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT uq_inscripciones UNIQUE (id_matricula, id_grupo);


--
-- Name: materias uq_materias_codigo_nombre; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT uq_materias_codigo_nombre UNIQUE (codigo_materia, nombre_materia);


--
-- Name: materias_prerrequisitos uq_materias_prerrequisitos; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias_prerrequisitos
    ADD CONSTRAINT uq_materias_prerrequisitos UNIQUE (id_materia, id_materia_requerida);


--
-- Name: matriculas uq_matricula_estudiante_ciclo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT uq_matricula_estudiante_ciclo UNIQUE (id_estudiante, id_ciclo);


--
-- Name: matriculas uq_matriculas; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT uq_matriculas UNIQUE (id_estudiante, id_ciclo);


--
-- Name: pagos_cargos uq_pago_cargo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_cargos
    ADD CONSTRAINT uq_pago_cargo UNIQUE (id_pago, id_cargo);


--
-- Name: pensum uq_pensum_carrera_version; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum
    ADD CONSTRAINT uq_pensum_carrera_version UNIQUE (id_carrera, version);


--
-- Name: pensum_materias uq_pensum_materia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum_materias
    ADD CONSTRAINT uq_pensum_materia UNIQUE (id_pensum, id_materia);


--
-- Name: pensum_materias uq_pensum_materias; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum_materias
    ADD CONSTRAINT uq_pensum_materias UNIQUE (id_pensum, id_materia);


--
-- Name: periodos_academicos uq_periodo_ciclo_tipo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT uq_periodo_ciclo_tipo UNIQUE (id_ciclo, id_tipo_periodo);


--
-- Name: periodos_academicos uq_periodos_academicos; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT uq_periodos_academicos UNIQUE (id_ciclo, id_tipo_periodo);


--
-- Name: periodos_evaluacion uq_periodos_evaluacion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_evaluacion
    ADD CONSTRAINT uq_periodos_evaluacion UNIQUE (id_ciclo, periodo);


--
-- Name: periodos_tipos_evaluacion uq_periodos_tipos_evaluacion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_tipos_evaluacion
    ADD CONSTRAINT uq_periodos_tipos_evaluacion UNIQUE (id_periodo_evaluacion, id_tipo_evaluacion);


--
-- Name: personas uq_personas_documento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT uq_personas_documento UNIQUE (id_tipo_documento, numero_documento);


--
-- Name: materias_prerrequisitos uq_prerrequisito; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias_prerrequisitos
    ADD CONSTRAINT uq_prerrequisito UNIQUE (id_materia, id_materia_requerida);


--
-- Name: puntos_recaudo uq_punto_recaudo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.puntos_recaudo
    ADD CONSTRAINT uq_punto_recaudo UNIQUE (id_sede, punto_recaudo);


--
-- Name: usuarios usuarios_correo_institucional_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_correo_institucional_key UNIQUE (correo_institucional);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);


--
-- Name: idx_asistencias_clase; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asistencias_clase ON public.asistencias USING btree (id_clase);


--
-- Name: idx_asistencias_inscripcion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asistencias_inscripcion ON public.asistencias USING btree (id_inscripcion);


--
-- Name: idx_auditoria_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_fecha ON public.auditoria USING btree (fecha_hora DESC);


--
-- Name: idx_auditoria_tabla_registro; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_tabla_registro ON public.auditoria USING btree (tabla, id_registro);


--
-- Name: idx_auditoria_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_usuario ON public.auditoria USING btree (id_usuario);


--
-- Name: idx_aulas_edificio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_aulas_edificio ON public.aulas USING btree (id_edificio);


--
-- Name: idx_calificaciones_evaluacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_evaluacion ON public.calificaciones USING btree (id_evaluacion);


--
-- Name: idx_calificaciones_inscripcion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_inscripcion ON public.calificaciones USING btree (id_inscripcion);


--
-- Name: idx_cargos_concepto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cargos_concepto ON public.cargos_estudiante USING btree (id_concepto_cobro);


--
-- Name: idx_cargos_matricula; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cargos_matricula ON public.cargos_estudiante USING btree (id_matricula);


--
-- Name: idx_carreras_facultad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_carreras_facultad ON public.carreras USING btree (id_facultad);


--
-- Name: idx_carreras_sedes_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_carreras_sedes_sede ON public.carreras_sedes USING btree (id_sede);


--
-- Name: idx_carreras_sedes_sede_carrera; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_carreras_sedes_sede_carrera ON public.carreras_sedes USING btree (id_sede, id_carrera);


--
-- Name: idx_clases_grupo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clases_grupo ON public.clases USING btree (id_grupo);


--
-- Name: idx_distritos_departamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_distritos_departamento ON public.distritos USING btree (id_departamento);


--
-- Name: idx_docentes_contratacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_docentes_contratacion ON public.docentes USING btree (id_tipo_contratacion);


--
-- Name: idx_docentes_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_docentes_sede ON public.docentes USING btree (id_sede);


--
-- Name: idx_edificios_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_edificios_sede ON public.edificios USING btree (id_sede);


--
-- Name: idx_empleados_area; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_empleados_area ON public.empleados USING btree (id_area);


--
-- Name: idx_empleados_cargo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_empleados_cargo ON public.empleados USING btree (id_cargo);


--
-- Name: idx_empleados_id_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_empleados_id_sede ON public.empleados USING btree (id_sede);


--
-- Name: idx_estudiantes_carreras_estudiante; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estudiantes_carreras_estudiante ON public.estudiantes_carreras USING btree (id_estudiante);


--
-- Name: idx_evaluaciones_grupo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluaciones_grupo ON public.evaluaciones USING btree (id_grupo);


--
-- Name: idx_facultades_sedes_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_facultades_sedes_sede ON public.facultades_sedes USING btree (id_sede);


--
-- Name: idx_grupos_ciclo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_ciclo ON public.grupos USING btree (id_ciclo);


--
-- Name: idx_grupos_ciclo_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_ciclo_sede ON public.grupos USING btree (id_ciclo, id_sede);


--
-- Name: idx_grupos_docente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_docente ON public.grupos USING btree (id_docente);


--
-- Name: idx_grupos_docente_ciclo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_docente_ciclo ON public.grupos USING btree (id_docente, id_ciclo);


--
-- Name: idx_grupos_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_estado ON public.grupos USING btree (id_estado_grupo);


--
-- Name: idx_grupos_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_materia ON public.grupos USING btree (id_materia);


--
-- Name: idx_grupos_plantilla; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_plantilla ON public.grupos USING btree (id_plantilla);


--
-- Name: idx_grupos_sede; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grupos_sede ON public.grupos USING btree (id_sede);


--
-- Name: idx_horarios_grupo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_grupo ON public.horarios USING btree (id_grupo);


--
-- Name: idx_inscripciones_grupo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inscripciones_grupo ON public.inscripciones USING btree (id_grupo);


--
-- Name: idx_inscripciones_matricula; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inscripciones_matricula ON public.inscripciones USING btree (id_matricula);


--
-- Name: idx_intentos_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_intentos_fecha ON public.intentos_acceso USING btree (fecha_hora);


--
-- Name: idx_intentos_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_intentos_usuario ON public.intentos_acceso USING btree (id_usuario);


--
-- Name: idx_matriculas_ciclo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matriculas_ciclo ON public.matriculas USING btree (id_ciclo);


--
-- Name: idx_matriculas_estudiante; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matriculas_estudiante ON public.matriculas USING btree (id_estudiante);


--
-- Name: idx_mv_dashboard_ciclo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_mv_dashboard_ciclo ON public.mv_dashboard_gerencial USING btree (id_ciclo);


--
-- Name: idx_pagos_cargos_cargo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagos_cargos_cargo ON public.pagos_cargos USING btree (id_cargo);


--
-- Name: idx_pagos_cargos_pago; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagos_cargos_pago ON public.pagos_cargos USING btree (id_pago);


--
-- Name: idx_pagos_estudiante; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagos_estudiante ON public.pagos USING btree (id_estudiante);


--
-- Name: idx_pensum_carrera; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pensum_carrera ON public.pensum USING btree (id_carrera);


--
-- Name: idx_pensum_materias_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pensum_materias_materia ON public.pensum_materias USING btree (id_materia);


--
-- Name: idx_pensum_materias_pensum_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pensum_materias_pensum_materia ON public.pensum_materias USING btree (id_pensum, id_materia);


--
-- Name: idx_pensums_carrera_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pensums_carrera_estado ON public.pensum USING btree (id_carrera, id_estado_pensum);


--
-- Name: idx_personas_distrito; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_personas_distrito ON public.personas USING btree (id_distrito);


--
-- Name: idx_personas_estado_registro; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_personas_estado_registro ON public.personas USING btree (id_estado_registro);


--
-- Name: idx_prerrequisitos_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_prerrequisitos_materia ON public.materias_prerrequisitos USING btree (id_materia);


--
-- Name: idx_prerrequisitos_requerida; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_prerrequisitos_requerida ON public.materias_prerrequisitos USING btree (id_materia_requerida);


--
-- Name: idx_sesiones_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sesiones_usuario ON public.sesiones_usuario USING btree (id_usuario);


--
-- Name: idx_solicitudes_calificacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_calificacion ON public.solicitudes_calificacion USING btree (id_calificacion);


--
-- Name: idx_usuarios_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_estado ON public.usuarios USING btree (id_estado_usuario);


--
-- Name: idx_usuarios_rol; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_rol ON public.usuarios USING btree (id_rol);


--
-- Name: uq_trayectoria_activa; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_trayectoria_activa ON public.estudiantes_carreras USING btree (id_estudiante) WHERE (fecha_fin IS NULL);


--
-- Name: asistencias trg_validar_asistencia_integridad; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_asistencia_integridad BEFORE INSERT OR UPDATE ON public.asistencias FOR EACH ROW EXECUTE FUNCTION public.fn_validar_asistencia_integridad();


--
-- Name: calificaciones trg_validar_calificacion_integridad; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_calificacion_integridad BEFORE INSERT OR UPDATE ON public.calificaciones FOR EACH ROW EXECUTE FUNCTION public.fn_validar_calificacion_integridad();


--
-- Name: inscripciones trg_validar_inscripcion_integridad; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_inscripcion_integridad BEFORE INSERT OR UPDATE ON public.inscripciones FOR EACH ROW EXECUTE FUNCTION public.fn_validar_inscripcion_integridad();


--
-- Name: areas fk_areas_padre; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas
    ADD CONSTRAINT fk_areas_padre FOREIGN KEY (id_area_padre) REFERENCES public.areas(id_area);


--
-- Name: asistencias fk_asistencias_clase; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT fk_asistencias_clase FOREIGN KEY (id_clase) REFERENCES public.clases(id_clase);


--
-- Name: asistencias fk_asistencias_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT fk_asistencias_estado FOREIGN KEY (id_estado_asistencia) REFERENCES public.estados_asistencia(id_estado_asistencia);


--
-- Name: asistencias fk_asistencias_inscripcion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT fk_asistencias_inscripcion FOREIGN KEY (id_inscripcion) REFERENCES public.inscripciones(id_inscripcion);


--
-- Name: auditoria fk_auditoria_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE SET NULL;


--
-- Name: aulas fk_aulas_edificio; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT fk_aulas_edificio FOREIGN KEY (id_edificio) REFERENCES public.edificios(id_edificio);


--
-- Name: aulas fk_aulas_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT fk_aulas_estado FOREIGN KEY (id_estado_aula) REFERENCES public.estados_aula(id_estado_aula);


--
-- Name: aulas fk_aulas_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT fk_aulas_tipo FOREIGN KEY (id_tipo_aula) REFERENCES public.tipos_aula(id_tipo_aula);


--
-- Name: beneficios_cobro fk_beneficios_cobro_beneficio; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios_cobro
    ADD CONSTRAINT fk_beneficios_cobro_beneficio FOREIGN KEY (id_beneficio) REFERENCES public.beneficios(id_beneficio);


--
-- Name: beneficios_cobro fk_beneficios_cobro_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios_cobro
    ADD CONSTRAINT fk_beneficios_cobro_tipo FOREIGN KEY (id_tipo_cobro) REFERENCES public.tipos_cobro(id_tipo_cobro);


--
-- Name: beneficios fk_beneficios_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficios
    ADD CONSTRAINT fk_beneficios_estado FOREIGN KEY (id_estado_beneficio) REFERENCES public.estados_beneficio(id_estado_beneficio);


--
-- Name: calificaciones fk_calificaciones_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT fk_calificaciones_estado FOREIGN KEY (id_estado_calificacion) REFERENCES public.estados_calificacion(id_estado_calificacion);


--
-- Name: calificaciones fk_calificaciones_evaluacion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT fk_calificaciones_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES public.evaluaciones(id_evaluacion);


--
-- Name: calificaciones fk_calificaciones_inscripcion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT fk_calificaciones_inscripcion FOREIGN KEY (id_inscripcion) REFERENCES public.inscripciones(id_inscripcion);


--
-- Name: calificaciones fk_calificaciones_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones
    ADD CONSTRAINT fk_calificaciones_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario);


--
-- Name: cargos_estudiante fk_cargos_beneficio; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos_estudiante
    ADD CONSTRAINT fk_cargos_beneficio FOREIGN KEY (id_estudiante_beneficio) REFERENCES public.estudiantes_beneficios(id_estudiante_beneficio);


--
-- Name: cargos_estudiante fk_cargos_concepto; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos_estudiante
    ADD CONSTRAINT fk_cargos_concepto FOREIGN KEY (id_concepto_cobro) REFERENCES public.conceptos_cobro(id_concepto_cobro);


--
-- Name: cargos_estudiante fk_cargos_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos_estudiante
    ADD CONSTRAINT fk_cargos_estado FOREIGN KEY (id_estado_cargo) REFERENCES public.estados_cargo(id_estado_cargo);


--
-- Name: cargos_estudiante fk_cargos_matricula; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cargos_estudiante
    ADD CONSTRAINT fk_cargos_matricula FOREIGN KEY (id_matricula) REFERENCES public.matriculas(id_matricula);


--
-- Name: carreras fk_carreras_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras
    ADD CONSTRAINT fk_carreras_estado FOREIGN KEY (id_estado_carrera) REFERENCES public.estados_carrera(id_estado_carrera);


--
-- Name: carreras fk_carreras_facultad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras
    ADD CONSTRAINT fk_carreras_facultad FOREIGN KEY (id_facultad) REFERENCES public.facultades(id_facultad);


--
-- Name: carreras fk_carreras_nivel; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras
    ADD CONSTRAINT fk_carreras_nivel FOREIGN KEY (id_nivel) REFERENCES public.niveles_academicos(id_nivel);


--
-- Name: carreras_sedes fk_carreras_sedes_carrera; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras_sedes
    ADD CONSTRAINT fk_carreras_sedes_carrera FOREIGN KEY (id_carrera) REFERENCES public.carreras(id_carrera);


--
-- Name: carreras_sedes fk_carreras_sedes_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carreras_sedes
    ADD CONSTRAINT fk_carreras_sedes_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede);


--
-- Name: ciclos fk_ciclos_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ciclos
    ADD CONSTRAINT fk_ciclos_estado FOREIGN KEY (id_estado_ciclo) REFERENCES public.estados_ciclo(id_estado_ciclo);


--
-- Name: clases fk_clases_grupo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clases
    ADD CONSTRAINT fk_clases_grupo FOREIGN KEY (id_grupo) REFERENCES public.grupos(id_grupo);


--
-- Name: conceptos_cobro fk_conceptos_ciclo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT fk_conceptos_ciclo FOREIGN KEY (id_ciclo) REFERENCES public.ciclos(id_ciclo);


--
-- Name: conceptos_cobro fk_conceptos_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT fk_conceptos_tipo FOREIGN KEY (id_tipo_cobro) REFERENCES public.tipos_cobro(id_tipo_cobro);


--
-- Name: distritos fk_distritos_departamentos; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distritos
    ADD CONSTRAINT fk_distritos_departamentos FOREIGN KEY (id_departamento) REFERENCES public.departamentos(id_departamento) ON DELETE RESTRICT;


--
-- Name: docentes fk_docentes_contratacion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT fk_docentes_contratacion FOREIGN KEY (id_tipo_contratacion) REFERENCES public.tipos_contratacion_docente(id_tipo_contratacion);


--
-- Name: docentes fk_docentes_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT fk_docentes_estado FOREIGN KEY (id_estado_docente) REFERENCES public.estados_docente(id_estado_docente);


--
-- Name: docentes fk_docentes_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT fk_docentes_persona FOREIGN KEY (id_persona) REFERENCES public.personas(id_persona);


--
-- Name: docentes fk_docentes_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT fk_docentes_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede);


--
-- Name: docentes fk_docentes_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.docentes
    ADD CONSTRAINT fk_docentes_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario);


--
-- Name: edificios fk_edificios_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.edificios
    ADD CONSTRAINT fk_edificios_estado FOREIGN KEY (id_estado_edificio) REFERENCES public.estados_edificio(id_estado_edificio);


--
-- Name: edificios fk_edificios_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.edificios
    ADD CONSTRAINT fk_edificios_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede);


--
-- Name: empleados fk_empleados_area; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT fk_empleados_area FOREIGN KEY (id_area) REFERENCES public.areas(id_area);


--
-- Name: empleados fk_empleados_cargo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT fk_empleados_cargo FOREIGN KEY (id_cargo) REFERENCES public.cargos(id_cargo);


--
-- Name: empleados fk_empleados_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT fk_empleados_estado FOREIGN KEY (id_estado_empleado) REFERENCES public.estados_empleado(id_estado_empleado);


--
-- Name: empleados fk_empleados_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT fk_empleados_persona FOREIGN KEY (id_persona) REFERENCES public.personas(id_persona);


--
-- Name: empleados fk_empleados_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT fk_empleados_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede) ON DELETE RESTRICT;


--
-- Name: empleados fk_empleados_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.empleados
    ADD CONSTRAINT fk_empleados_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario);


--
-- Name: estudiantes_beneficios fk_estudiantes_beneficios_beneficio; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_beneficios
    ADD CONSTRAINT fk_estudiantes_beneficios_beneficio FOREIGN KEY (id_beneficio) REFERENCES public.beneficios(id_beneficio);


--
-- Name: estudiantes_beneficios fk_estudiantes_beneficios_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_beneficios
    ADD CONSTRAINT fk_estudiantes_beneficios_estado FOREIGN KEY (id_estado_beneficio_estudiante) REFERENCES public.estados_beneficio_estudiante(id_estado_beneficio_estudiante);


--
-- Name: estudiantes_beneficios fk_estudiantes_beneficios_estudiante; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_beneficios
    ADD CONSTRAINT fk_estudiantes_beneficios_estudiante FOREIGN KEY (id_estudiante) REFERENCES public.estudiantes(id_estudiante);


--
-- Name: estudiantes_beneficios fk_estudiantes_beneficios_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_beneficios
    ADD CONSTRAINT fk_estudiantes_beneficios_usuario FOREIGN KEY (id_usuario_aprueba) REFERENCES public.usuarios(id_usuario);


--
-- Name: estudiantes_carreras fk_estudiantes_carreras_carrera; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_carreras
    ADD CONSTRAINT fk_estudiantes_carreras_carrera FOREIGN KEY (id_carrera_sede) REFERENCES public.carreras_sedes(id_carrera_sede);


--
-- Name: estudiantes_carreras fk_estudiantes_carreras_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_carreras
    ADD CONSTRAINT fk_estudiantes_carreras_estado FOREIGN KEY (id_estado_trayectoria) REFERENCES public.estados_trayectoria(id_estado_trayectoria);


--
-- Name: estudiantes_carreras fk_estudiantes_carreras_estudiante; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_carreras
    ADD CONSTRAINT fk_estudiantes_carreras_estudiante FOREIGN KEY (id_estudiante) REFERENCES public.estudiantes(id_estudiante);


--
-- Name: estudiantes_carreras fk_estudiantes_carreras_pensum; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes_carreras
    ADD CONSTRAINT fk_estudiantes_carreras_pensum FOREIGN KEY (id_pensum) REFERENCES public.pensum(id_pensum);


--
-- Name: estudiantes fk_estudiantes_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT fk_estudiantes_estado FOREIGN KEY (id_estado_estudiante) REFERENCES public.estados_estudiante(id_estado_estudiante);


--
-- Name: estudiantes fk_estudiantes_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT fk_estudiantes_persona FOREIGN KEY (id_persona) REFERENCES public.personas(id_persona);


--
-- Name: estudiantes fk_estudiantes_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT fk_estudiantes_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario);


--
-- Name: evaluaciones fk_evaluaciones_grupo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT fk_evaluaciones_grupo FOREIGN KEY (id_grupo) REFERENCES public.grupos(id_grupo);


--
-- Name: evaluaciones fk_evaluaciones_periodo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT fk_evaluaciones_periodo FOREIGN KEY (id_periodo_evaluacion) REFERENCES public.periodos_evaluacion(id_periodo_evaluacion);


--
-- Name: evaluaciones fk_evaluaciones_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT fk_evaluaciones_tipo FOREIGN KEY (id_tipo_evaluacion) REFERENCES public.tipos_evaluacion(id_tipo_evaluacion);


--
-- Name: facturas fk_facturas_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facturas
    ADD CONSTRAINT fk_facturas_estado FOREIGN KEY (id_estado_factura) REFERENCES public.estados_factura(id_estado_factura);


--
-- Name: facturas fk_facturas_pago; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facturas
    ADD CONSTRAINT fk_facturas_pago FOREIGN KEY (id_pago) REFERENCES public.pagos(id_pago);


--
-- Name: facultades fk_facultades_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades
    ADD CONSTRAINT fk_facultades_estado FOREIGN KEY (id_estado_facultad) REFERENCES public.estados_facultad(id_estado_facultad);


--
-- Name: facultades_sedes fk_facultades_sedes_facultad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades_sedes
    ADD CONSTRAINT fk_facultades_sedes_facultad FOREIGN KEY (id_facultad) REFERENCES public.facultades(id_facultad);


--
-- Name: facultades_sedes fk_facultades_sedes_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.facultades_sedes
    ADD CONSTRAINT fk_facultades_sedes_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede);


--
-- Name: grupos fk_grupos_ciclo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT fk_grupos_ciclo FOREIGN KEY (id_ciclo) REFERENCES public.ciclos(id_ciclo);


--
-- Name: grupos fk_grupos_docente; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT fk_grupos_docente FOREIGN KEY (id_docente) REFERENCES public.docentes(id_docente);


--
-- Name: grupos fk_grupos_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT fk_grupos_estado FOREIGN KEY (id_estado_grupo) REFERENCES public.estados_grupo(id_estado_grupo);


--
-- Name: grupos fk_grupos_materia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT fk_grupos_materia FOREIGN KEY (id_materia) REFERENCES public.materias(id_materia);


--
-- Name: grupos fk_grupos_plantilla; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT fk_grupos_plantilla FOREIGN KEY (id_plantilla) REFERENCES public.plantillas_horarios(id_plantilla);


--
-- Name: grupos fk_grupos_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT fk_grupos_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede);


--
-- Name: horarios fk_horarios_aula; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT fk_horarios_aula FOREIGN KEY (id_aula) REFERENCES public.aulas(id_aula);


--
-- Name: horarios fk_horarios_dia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT fk_horarios_dia FOREIGN KEY (id_dia) REFERENCES public.dias_semana(id_dia);


--
-- Name: horarios fk_horarios_grupo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT fk_horarios_grupo FOREIGN KEY (id_grupo) REFERENCES public.grupos(id_grupo);


--
-- Name: horarios fk_horarios_modalidad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT fk_horarios_modalidad FOREIGN KEY (id_modalidad) REFERENCES public.modalidades(id_modalidad);


--
-- Name: inscripciones fk_inscripciones_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT fk_inscripciones_estado FOREIGN KEY (id_estado_inscripcion) REFERENCES public.estados_inscripcion(id_estado_inscripcion);


--
-- Name: inscripciones fk_inscripciones_estado_materia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT fk_inscripciones_estado_materia FOREIGN KEY (id_estado_materia) REFERENCES public.estados_materia_estudiante(id_estado_materia);


--
-- Name: inscripciones fk_inscripciones_grupo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT fk_inscripciones_grupo FOREIGN KEY (id_grupo) REFERENCES public.grupos(id_grupo);


--
-- Name: inscripciones fk_inscripciones_matricula; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT fk_inscripciones_matricula FOREIGN KEY (id_matricula) REFERENCES public.matriculas(id_matricula);


--
-- Name: inscripciones fk_inscripciones_periodo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones
    ADD CONSTRAINT fk_inscripciones_periodo FOREIGN KEY (id_periodo) REFERENCES public.periodos_academicos(id_periodo);


--
-- Name: intentos_acceso fk_intentos_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intentos_acceso
    ADD CONSTRAINT fk_intentos_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE SET NULL;


--
-- Name: matriculas fk_matriculas_carrera; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT fk_matriculas_carrera FOREIGN KEY (id_estudiante_carrera) REFERENCES public.estudiantes_carreras(id_estudiante_carrera);


--
-- Name: matriculas fk_matriculas_ciclo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT fk_matriculas_ciclo FOREIGN KEY (id_ciclo) REFERENCES public.ciclos(id_ciclo);


--
-- Name: matriculas fk_matriculas_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT fk_matriculas_estado FOREIGN KEY (id_estado_matricula) REFERENCES public.estados_matricula(id_estado_matricula);


--
-- Name: matriculas fk_matriculas_estudiante; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT fk_matriculas_estudiante FOREIGN KEY (id_estudiante) REFERENCES public.estudiantes(id_estudiante);


--
-- Name: pagos fk_pagos_canal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_canal FOREIGN KEY (id_canal_pago) REFERENCES public.canales_pago(id_canal_pago);


--
-- Name: pagos_cargos fk_pagos_cargos_cargo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_cargos
    ADD CONSTRAINT fk_pagos_cargos_cargo FOREIGN KEY (id_cargo) REFERENCES public.cargos_estudiante(id_cargo_estudiante);


--
-- Name: pagos_cargos fk_pagos_cargos_pago; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_cargos
    ADD CONSTRAINT fk_pagos_cargos_pago FOREIGN KEY (id_pago) REFERENCES public.pagos(id_pago);


--
-- Name: pagos fk_pagos_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_estado FOREIGN KEY (id_estado_pago) REFERENCES public.estados_pago(id_estado_pago);


--
-- Name: pagos fk_pagos_estudiante; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_estudiante FOREIGN KEY (id_estudiante) REFERENCES public.estudiantes(id_estudiante);


--
-- Name: pagos fk_pagos_metodo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_metodo FOREIGN KEY (id_metodo_pago) REFERENCES public.metodos_pago(id_metodo_pago);


--
-- Name: pagos fk_pagos_punto; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_punto FOREIGN KEY (id_punto_recaudo) REFERENCES public.puntos_recaudo(id_punto_recaudo);


--
-- Name: pagos fk_pagos_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_usuario FOREIGN KEY (id_usuario_registro) REFERENCES public.usuarios(id_usuario);


--
-- Name: pensum fk_pensum_carrera; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum
    ADD CONSTRAINT fk_pensum_carrera FOREIGN KEY (id_carrera) REFERENCES public.carreras(id_carrera);


--
-- Name: pensum fk_pensum_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum
    ADD CONSTRAINT fk_pensum_estado FOREIGN KEY (id_estado_pensum) REFERENCES public.estados_pensum(id_estado_pensum);


--
-- Name: pensum_materias fk_pensum_materias_materia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum_materias
    ADD CONSTRAINT fk_pensum_materias_materia FOREIGN KEY (id_materia) REFERENCES public.materias(id_materia);


--
-- Name: pensum_materias fk_pensum_materias_pensum; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pensum_materias
    ADD CONSTRAINT fk_pensum_materias_pensum FOREIGN KEY (id_pensum) REFERENCES public.pensum(id_pensum);


--
-- Name: periodos_academicos fk_periodos_ciclo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT fk_periodos_ciclo FOREIGN KEY (id_ciclo) REFERENCES public.ciclos(id_ciclo);


--
-- Name: periodos_evaluacion fk_periodos_evaluacion_ciclo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_evaluacion
    ADD CONSTRAINT fk_periodos_evaluacion_ciclo FOREIGN KEY (id_ciclo) REFERENCES public.ciclos(id_ciclo);


--
-- Name: periodos_academicos fk_periodos_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT fk_periodos_tipo FOREIGN KEY (id_tipo_periodo) REFERENCES public.tipos_periodo(id_tipo_periodo);


--
-- Name: periodos_tipos_evaluacion fk_periodos_tipos_evaluacion_periodo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_tipos_evaluacion
    ADD CONSTRAINT fk_periodos_tipos_evaluacion_periodo FOREIGN KEY (id_periodo_evaluacion) REFERENCES public.periodos_evaluacion(id_periodo_evaluacion);


--
-- Name: periodos_tipos_evaluacion fk_periodos_tipos_evaluacion_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_tipos_evaluacion
    ADD CONSTRAINT fk_periodos_tipos_evaluacion_tipo FOREIGN KEY (id_tipo_evaluacion) REFERENCES public.tipos_evaluacion(id_tipo_evaluacion);


--
-- Name: personas fk_personas_distritos; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT fk_personas_distritos FOREIGN KEY (id_distrito) REFERENCES public.distritos(id_distrito) ON DELETE RESTRICT;


--
-- Name: personas fk_personas_estado_registro; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT fk_personas_estado_registro FOREIGN KEY (id_estado_registro) REFERENCES public.estados_registro_persona(id_estado_registro);


--
-- Name: personas fk_personas_tipo_documento; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT fk_personas_tipo_documento FOREIGN KEY (id_tipo_documento) REFERENCES public.tipos_documento(id_tipo_documento);


--
-- Name: plantillas_horarios_detalle fk_plantilla_detalle_dia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plantillas_horarios_detalle
    ADD CONSTRAINT fk_plantilla_detalle_dia FOREIGN KEY (id_dia) REFERENCES public.dias_semana(id_dia);


--
-- Name: plantillas_horarios_detalle fk_plantilla_detalle_plantilla; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plantillas_horarios_detalle
    ADD CONSTRAINT fk_plantilla_detalle_plantilla FOREIGN KEY (id_plantilla) REFERENCES public.plantillas_horarios(id_plantilla) ON DELETE CASCADE;


--
-- Name: materias_prerrequisitos fk_prerrequisitos_materia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias_prerrequisitos
    ADD CONSTRAINT fk_prerrequisitos_materia FOREIGN KEY (id_materia) REFERENCES public.materias(id_materia);


--
-- Name: materias_prerrequisitos fk_prerrequisitos_requerida; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias_prerrequisitos
    ADD CONSTRAINT fk_prerrequisitos_requerida FOREIGN KEY (id_materia_requerida) REFERENCES public.materias(id_materia);


--
-- Name: puntos_recaudo fk_puntos_recaudo_sede; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.puntos_recaudo
    ADD CONSTRAINT fk_puntos_recaudo_sede FOREIGN KEY (id_sede) REFERENCES public.sedes(id_sede);


--
-- Name: recargos fk_recargos_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recargos
    ADD CONSTRAINT fk_recargos_estado FOREIGN KEY (id_estado_recargo) REFERENCES public.estados_recargo(id_estado_recargo);


--
-- Name: recargos fk_recargos_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recargos
    ADD CONSTRAINT fk_recargos_tipo FOREIGN KEY (id_tipo_cobro) REFERENCES public.tipos_cobro(id_tipo_cobro);


--
-- Name: recuperaciones_clave fk_recuperaciones_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recuperaciones_clave
    ADD CONSTRAINT fk_recuperaciones_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario);


--
-- Name: sedes fk_sedes_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sedes
    ADD CONSTRAINT fk_sedes_estado FOREIGN KEY (id_estado_sede) REFERENCES public.estados_sede(id_estado_sede);


--
-- Name: sedes fk_sedes_tipo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sedes
    ADD CONSTRAINT fk_sedes_tipo FOREIGN KEY (id_tipo_sede) REFERENCES public.tipos_sede(id_tipo_sede);


--
-- Name: sesiones_usuario fk_sesiones_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_usuario
    ADD CONSTRAINT fk_sesiones_rol FOREIGN KEY (id_rol) REFERENCES public.roles(id_rol);


--
-- Name: sesiones_usuario fk_sesiones_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_usuario
    ADD CONSTRAINT fk_sesiones_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario);


--
-- Name: solicitudes_calificacion fk_solicitudes_autorizador; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_calificacion
    ADD CONSTRAINT fk_solicitudes_autorizador FOREIGN KEY (id_usuario_autorizador) REFERENCES public.usuarios(id_usuario);


--
-- Name: solicitudes_calificacion fk_solicitudes_calificacion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_calificacion
    ADD CONSTRAINT fk_solicitudes_calificacion FOREIGN KEY (id_calificacion) REFERENCES public.calificaciones(id_calificacion);


--
-- Name: solicitudes_calificacion fk_solicitudes_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_calificacion
    ADD CONSTRAINT fk_solicitudes_estado FOREIGN KEY (id_estado_solicitud) REFERENCES public.estados_solicitud(id_estado_solicitud);


--
-- Name: solicitudes_calificacion fk_solicitudes_solicitante; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_calificacion
    ADD CONSTRAINT fk_solicitudes_solicitante FOREIGN KEY (id_usuario_solicitante) REFERENCES public.usuarios(id_usuario);


--
-- Name: usuarios fk_usuarios_estado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT fk_usuarios_estado FOREIGN KEY (id_estado_usuario) REFERENCES public.estados_usuario(id_estado_usuario);


--
-- Name: usuarios fk_usuarios_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT fk_usuarios_persona FOREIGN KEY (id_persona) REFERENCES public.personas(id_persona);


--
-- Name: usuarios fk_usuarios_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT fk_usuarios_rol FOREIGN KEY (id_rol) REFERENCES public.roles(id_rol);


--
-- Name: estados_materia_estudiante; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.estados_materia_estudiante ENABLE ROW LEVEL SECURITY;

--
-- Name: periodos_evaluacion; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.periodos_evaluacion ENABLE ROW LEVEL SECURITY;

--
-- Name: periodos_tipos_evaluacion; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.periodos_tipos_evaluacion ENABLE ROW LEVEL SECURITY;

--
-- Name: tipos_documento; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.tipos_documento ENABLE ROW LEVEL SECURITY;

--
-- PostgreSQL database dump complete
--

\unrestrict JDm4M4mstz5mrwRSxpT3styE9oZzcufkduiEVN41c4cMKO7g5HxYiuQ6JZUnIal

