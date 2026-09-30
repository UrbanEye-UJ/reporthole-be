-- Baseline migration: captures the schema Hibernate's `ddl-auto: update` had already built up to
-- this point (generated via `pg_dump --schema-only` against the running local/dev database, then
-- stripped of owner-specific `ALTER ... OWNER TO` statements and the `public` schema's own
-- create/comment, since a fresh database already has that schema and this must run under any
-- role). From here on, schema changes are versioned Flyway migrations (V2, V3, ...) instead of
-- Hibernate auto-generating DDL — see `spring.jpa.hibernate.ddl-auto: validate` in
-- application-local.yml / application-prod.yml. Requires the `postgis` extension to already be
-- installed (done once by `scripts/init-db.sql` before the application ever starts).

CREATE TABLE public.access_control_audit (
    access_control_audit_id uuid NOT NULL,
    access_control_audit_action character varying(255) NOT NULL,
    access_control_audit_created_at timestamp(6) without time zone NOT NULL,
    access_control_audit_from_value character varying(255),
    access_control_audit_reason character varying(500),
    access_control_audit_to_value character varying(255),
    access_control_audit_actor uuid NOT NULL,
    access_control_audit_target uuid NOT NULL,
    CONSTRAINT access_control_audit_access_control_audit_action_check CHECK (((access_control_audit_action)::text = ANY ((ARRAY['ROLE_GRANTED'::character varying, 'ROLE_REVOKED'::character varying, 'ACCOUNT_SUSPENDED'::character varying, 'ACCOUNT_REACTIVATED'::character varying, 'SESSIONS_REVOKED'::character varying, 'USER_LOGIN'::character varying, 'USER_REGISTERED'::character varying, 'PII_REVEALED'::character varying, 'ACCOUNT_LOCKED'::character varying])::text[])))
);

CREATE TABLE public.admin_applications (
    application_id uuid NOT NULL,
    municipality_token character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    submitted_at timestamp(6) without time zone NOT NULL,
    municipality_id uuid,
    user_id uuid NOT NULL,
    CONSTRAINT admin_applications_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);

CREATE TABLE public.assignment (
    assignment_id uuid NOT NULL,
    assignment_date timestamp(6) without time zone NOT NULL,
    assignment_completion_date timestamp(6) without time zone,
    assignment_resolution_image_url character varying(255),
    assignment_resolution_notes character varying(500),
    assignment_status character varying(255) NOT NULL,
    assignment_contractor_user_id uuid NOT NULL,
    assignment_incident_id uuid NOT NULL,
    CONSTRAINT assignment_assignment_status_check CHECK (((assignment_status)::text = ANY ((ARRAY['REPORTED'::character varying, 'VERIFIED'::character varying, 'ASSIGNED'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying])::text[])))
);

CREATE TABLE public.assignment_workflow (
    assignment_workflow_id uuid NOT NULL,
    assignment_workflow_notes character varying(255),
    assignment_workflow_status character varying(255) NOT NULL,
    assignment_workflow_updated_date timestamp(6) without time zone NOT NULL,
    assignment_workflow_incident uuid NOT NULL,
    assignment_workflow_updatedby uuid,
    CONSTRAINT assignment_workflow_assignment_workflow_status_check CHECK (((assignment_workflow_status)::text = ANY ((ARRAY['REPORTED'::character varying, 'VERIFIED'::character varying, 'ASSIGNED'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying])::text[])))
);

CREATE TABLE public.audit_log (
    audit_log_id uuid NOT NULL,
    audit_log_action character varying(100) NOT NULL,
    audit_log_created_at timestamp(6) without time zone NOT NULL,
    audit_log_entity_id uuid,
    audit_log_entity_type character varying(100) NOT NULL,
    audit_log_summary character varying(500) NOT NULL,
    audit_log_actor uuid
);

CREATE TABLE public.contractor_invite (
    invite_id uuid NOT NULL,
    invite_created_at timestamp(6) without time zone NOT NULL,
    invite_email character varying(255) NOT NULL,
    invite_email_hash character varying(255) NOT NULL,
    invite_expires_at timestamp(6) without time zone NOT NULL,
    invite_token uuid NOT NULL,
    invite_used boolean NOT NULL,
    invite_municipality_id uuid
);

CREATE TABLE public.contractor_invite_specialisation (
    invite_id uuid NOT NULL,
    issue_type character varying(255),
    CONSTRAINT contractor_invite_specialisation_issue_type_check CHECK (((issue_type)::text = ANY ((ARRAY['POTHOLE'::character varying, 'CRACK'::character varying, 'FADED_MARKINGS'::character varying, 'DAMAGED_SIGN'::character varying, 'BLOCKED_DRAIN'::character varying, 'BROKEN_TRAFFIC_LIGHT'::character varying, 'ACCIDENT'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.contractor_specialisation (
    user_id uuid NOT NULL,
    issue_type character varying(255),
    CONSTRAINT contractor_specialisation_issue_type_check CHECK (((issue_type)::text = ANY ((ARRAY['POTHOLE'::character varying, 'CRACK'::character varying, 'FADED_MARKINGS'::character varying, 'DAMAGED_SIGN'::character varying, 'BLOCKED_DRAIN'::character varying, 'BROKEN_TRAFFIC_LIGHT'::character varying, 'ACCIDENT'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.dashcam_device (
    device_id uuid NOT NULL,
    device_created_at timestamp(6) without time zone NOT NULL,
    device_token character varying(255) NOT NULL,
    device_user_id uuid NOT NULL
);

CREATE TABLE public.idempotency_key (
    idempotency_key_id uuid NOT NULL,
    idempotency_key_created_at timestamp(6) without time zone NOT NULL
);

CREATE TABLE public.image (
    image_id uuid NOT NULL,
    image_date timestamp(6) without time zone NOT NULL,
    image_url character varying(255) NOT NULL,
    image_incident_id uuid NOT NULL
);

CREATE TABLE public.incident (
    incident_id uuid NOT NULL,
    incident_ai_confidence double precision,
    incident_ai_generated boolean DEFAULT false NOT NULL,
    incident_deleted boolean NOT NULL,
    incident_description character varying(300) NOT NULL,
    incident_image_url character varying(255),
    incident_date timestamp(6) without time zone NOT NULL,
    incident_type character varying(255) NOT NULL,
    incident_location public.geometry(Point,4326) NOT NULL,
    incident_location_address character varying(300),
    incident_report_count integer NOT NULL,
    incident_source character varying(255) NOT NULL,
    incident_municipality_id uuid,
    incident_user_id uuid NOT NULL,
    incident_image_height integer,
    incident_image_width integer,
    incident_training_flagged_at timestamp(6) without time zone,
    incident_training_status character varying(20),
    CONSTRAINT incident_incident_source_check CHECK (((incident_source)::text = ANY ((ARRAY['MANUAL'::character varying, 'DASHCAM'::character varying])::text[]))),
    CONSTRAINT incident_incident_training_status_check CHECK (((incident_training_status)::text = ANY ((ARRAY['NOT_FLAGGED'::character varying, 'FLAGGED'::character varying, 'EXPORTED'::character varying])::text[]))),
    CONSTRAINT incident_incident_type_check CHECK (((incident_type)::text = ANY ((ARRAY['POTHOLE'::character varying, 'CRACK'::character varying, 'FADED_MARKINGS'::character varying, 'DAMAGED_SIGN'::character varying, 'BLOCKED_DRAIN'::character varying, 'BROKEN_TRAFFIC_LIGHT'::character varying, 'ACCIDENT'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.incident_comment (
    comment_id uuid NOT NULL,
    content character varying(500) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    author_id uuid NOT NULL,
    incident_id uuid NOT NULL
);

CREATE TABLE public.incident_reporter (
    ir_id uuid NOT NULL,
    ir_reported_at timestamp(6) without time zone NOT NULL,
    ir_incident_id uuid NOT NULL,
    ir_user_id uuid NOT NULL
);

CREATE TABLE public.issue_annotation (
    annotation_id uuid NOT NULL,
    annotation_auto_generated boolean NOT NULL,
    annotation_class_label character varying(255) NOT NULL,
    annotation_created_at timestamp(6) without time zone NOT NULL,
    annotation_height double precision NOT NULL,
    annotation_width double precision NOT NULL,
    annotation_x_center double precision NOT NULL,
    annotation_y_center double precision NOT NULL,
    annotation_annotated_by uuid,
    annotation_incident_id uuid NOT NULL,
    CONSTRAINT issue_annotation_annotation_class_label_check CHECK (((annotation_class_label)::text = ANY ((ARRAY['POTHOLE'::character varying, 'CRACK'::character varying, 'FADED_MARKINGS'::character varying, 'DAMAGED_SIGN'::character varying, 'BLOCKED_DRAIN'::character varying, 'BROKEN_TRAFFIC_LIGHT'::character varying, 'ACCIDENT'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.messages (
    msg_id uuid NOT NULL,
    msg_category character varying(255) NOT NULL,
    msg_content character varying(2000) NOT NULL,
    msg_created_at timestamp(6) without time zone NOT NULL,
    msg_read boolean NOT NULL,
    msg_sender_email character varying(320) NOT NULL,
    msg_sender_name character varying(200) NOT NULL,
    msg_sender_role character varying(255),
    msg_sender_user_id uuid,
    msg_subject character varying(200),
    CONSTRAINT messages_msg_category_check CHECK (((msg_category)::text = ANY ((ARRAY['USER_MESSAGE'::character varying, 'CONTACT_US'::character varying])::text[]))),
    CONSTRAINT messages_msg_sender_role_check CHECK (((msg_sender_role)::text = ANY ((ARRAY['CIVILIAN'::character varying, 'CONTRACTOR'::character varying, 'ADMIN'::character varying, 'SECURITY_ADMIN'::character varying])::text[])))
);

CREATE TABLE public.municipalities (
    municipality_id uuid NOT NULL,
    municipality_boundary public.geometry(MultiPolygon,4326),
    municipality_created_at timestamp(6) without time zone NOT NULL,
    municipality_name character varying(255) NOT NULL,
    municipality_province character varying(255) NOT NULL,
    municipality_created_by uuid
);

CREATE TABLE public.municipality_tokens (
    municipality_token_id uuid NOT NULL,
    municipality_token_expires_at timestamp(6) without time zone,
    municipality_token_issued_at timestamp(6) without time zone NOT NULL,
    municipality_token_note character varying(500),
    municipality_token_revoked_at timestamp(6) without time zone,
    municipality_token_value character varying(255) NOT NULL,
    municipality_token_issued_by uuid NOT NULL,
    municipality_token_municipality uuid NOT NULL
);

CREATE TABLE public.notification (
    notification_id uuid NOT NULL,
    notification_created_at timestamp(6) without time zone NOT NULL,
    notification_message character varying(255) NOT NULL,
    notification_read boolean NOT NULL,
    notification_user_id uuid NOT NULL
);

CREATE TABLE public.user_auth (
    auth_id uuid NOT NULL,
    user_created_at timestamp(6) without time zone NOT NULL,
    auth_credentials_valid_from timestamp(6) without time zone,
    auth_email character varying(255) NOT NULL,
    auth_email_hash character varying(255) NOT NULL,
    auth_password_hash character varying(255) NOT NULL,
    auth_reset_token character varying(255),
    auth_reset_token_expires_at timestamp(6) without time zone,
    auth_retries integer NOT NULL,
    auth_status character varying(255) NOT NULL,
    auth_verification_token character varying(255),
    auth_verification_token_expires_at timestamp(6) without time zone,
    CONSTRAINT user_auth_auth_status_check CHECK (((auth_status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'ACTIVE'::character varying, 'LOCKED'::character varying, 'SUSPENDED'::character varying, 'DELETED'::character varying])::text[])))
);

CREATE TABLE public.users (
    user_id uuid NOT NULL,
    user_created_at timestamp(6) without time zone NOT NULL,
    user_firstname character varying(255) NOT NULL,
    user_lastname character varying(255) NOT NULL,
    user_phonenumber character varying(255),
    user_role character varying(255) NOT NULL,
    municipality_id uuid,
    CONSTRAINT users_user_role_check CHECK (((user_role)::text = ANY ((ARRAY['CIVILIAN'::character varying, 'CONTRACTOR'::character varying, 'ADMIN'::character varying, 'SECURITY_ADMIN'::character varying])::text[])))
);

ALTER TABLE ONLY public.access_control_audit
    ADD CONSTRAINT access_control_audit_pkey PRIMARY KEY (access_control_audit_id);

ALTER TABLE ONLY public.admin_applications
    ADD CONSTRAINT admin_applications_pkey PRIMARY KEY (application_id);

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT assignment_pkey PRIMARY KEY (assignment_id);

ALTER TABLE ONLY public.assignment_workflow
    ADD CONSTRAINT assignment_workflow_pkey PRIMARY KEY (assignment_workflow_id);

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (audit_log_id);

ALTER TABLE ONLY public.contractor_invite
    ADD CONSTRAINT contractor_invite_pkey PRIMARY KEY (invite_id);

ALTER TABLE ONLY public.dashcam_device
    ADD CONSTRAINT dashcam_device_pkey PRIMARY KEY (device_id);

ALTER TABLE ONLY public.idempotency_key
    ADD CONSTRAINT idempotency_key_pkey PRIMARY KEY (idempotency_key_id);

ALTER TABLE ONLY public.image
    ADD CONSTRAINT image_pkey PRIMARY KEY (image_id);

ALTER TABLE ONLY public.incident_comment
    ADD CONSTRAINT incident_comment_pkey PRIMARY KEY (comment_id);

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT incident_pkey PRIMARY KEY (incident_id);

ALTER TABLE ONLY public.incident_reporter
    ADD CONSTRAINT incident_reporter_pkey PRIMARY KEY (ir_id);

ALTER TABLE ONLY public.issue_annotation
    ADD CONSTRAINT issue_annotation_pkey PRIMARY KEY (annotation_id);

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (msg_id);

ALTER TABLE ONLY public.municipalities
    ADD CONSTRAINT municipalities_pkey PRIMARY KEY (municipality_id);

ALTER TABLE ONLY public.municipality_tokens
    ADD CONSTRAINT municipality_tokens_pkey PRIMARY KEY (municipality_token_id);

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (notification_id);

ALTER TABLE ONLY public.user_auth
    ADD CONSTRAINT uk2mb1s11a1et0rl5kv2vfxl2ye UNIQUE (auth_email);

ALTER TABLE ONLY public.user_auth
    ADD CONSTRAINT uk5t58713ary300lyj8moqb9wmf UNIQUE (auth_email_hash);

ALTER TABLE ONLY public.incident_reporter
    ADD CONSTRAINT uk8gkllw4sev8ahvuqd7y67w2sc UNIQUE (ir_incident_id, ir_user_id);

ALTER TABLE ONLY public.municipalities
    ADD CONSTRAINT uk8xw5vksdew39n4buhxjliah41 UNIQUE (municipality_name);

ALTER TABLE ONLY public.user_auth
    ADD CONSTRAINT ukausgobcpniluk7qpuh81y5jqo UNIQUE (auth_verification_token);

ALTER TABLE ONLY public.contractor_invite
    ADD CONSTRAINT ukidpckb4fnna0ff29dda54y6h5 UNIQUE (invite_email_hash);

ALTER TABLE ONLY public.user_auth
    ADD CONSTRAINT ukllghy4ajl9d6fe1ow21pyq8uy UNIQUE (auth_reset_token);

ALTER TABLE ONLY public.contractor_invite
    ADD CONSTRAINT ukoqrqsa1n8r07ug53v8ah61hmi UNIQUE (invite_token);

ALTER TABLE ONLY public.municipality_tokens
    ADD CONSTRAINT ukqej69x920cqjrauxkk9trjsab UNIQUE (municipality_token_value);

ALTER TABLE ONLY public.user_auth
    ADD CONSTRAINT user_auth_pkey PRIMARY KEY (auth_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT fk11qo0wjxp68mw5sbav5qwcrvc FOREIGN KEY (incident_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.incident_comment
    ADD CONSTRAINT fk2if6cp719obp3ckrtix2eoixq FOREIGN KEY (incident_id) REFERENCES public.incident(incident_id);

ALTER TABLE ONLY public.admin_applications
    ADD CONSTRAINT fk3n47pbvjinmhkp7hlogtvt350 FOREIGN KEY (municipality_id) REFERENCES public.municipalities(municipality_id);

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT fk46069bubpj4uhe4xww1239kaw FOREIGN KEY (audit_log_actor) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.incident_reporter
    ADD CONSTRAINT fk4acdeok1m6g60gml0o6cbhu5w FOREIGN KEY (ir_incident_id) REFERENCES public.incident(incident_id);

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT fk4w0r1fmcfkb21pfqlqauiup6q FOREIGN KEY (assignment_contractor_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.assignment_workflow
    ADD CONSTRAINT fk8hi12xgcl0qj0tncmb7ayuo09 FOREIGN KEY (assignment_workflow_incident) REFERENCES public.incident(incident_id);

ALTER TABLE ONLY public.admin_applications
    ADD CONSTRAINT fkat01b6444slgc3ie173dynrbd FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.contractor_specialisation
    ADD CONSTRAINT fkbrfivjnp4blb28jbv2qe35xnr FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.incident_reporter
    ADD CONSTRAINT fkcwlhxcof93sk24atllno83khd FOREIGN KEY (ir_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.issue_annotation
    ADD CONSTRAINT fkdjk006qevm6r49ift8n022v5q FOREIGN KEY (annotation_annotated_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT fkem0v7ijtmxab8dldwtqxm9xmf FOREIGN KEY (assignment_incident_id) REFERENCES public.incident(incident_id);

ALTER TABLE ONLY public.municipality_tokens
    ADD CONSTRAINT fkeqw3n3k103w2e9q2cdeedcn60 FOREIGN KEY (municipality_token_municipality) REFERENCES public.municipalities(municipality_id);

ALTER TABLE ONLY public.contractor_invite_specialisation
    ADD CONSTRAINT fkflmjjdiktlccq4vfbfdl13hrt FOREIGN KEY (invite_id) REFERENCES public.contractor_invite(invite_id);

ALTER TABLE ONLY public.issue_annotation
    ADD CONSTRAINT fkg5ab4ykuxuwomy7qggvhx2bs0 FOREIGN KEY (annotation_incident_id) REFERENCES public.incident(incident_id);

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT fkgdothoguoub3aam3tgiw57ady FOREIGN KEY (incident_municipality_id) REFERENCES public.municipalities(municipality_id);

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT fkh2hul3hxp7qpg93o9qslxg1dm FOREIGN KEY (notification_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.access_control_audit
    ADD CONSTRAINT fki57r26ut1rpjxtktdsw4s8a5m FOREIGN KEY (access_control_audit_target) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.assignment_workflow
    ADD CONSTRAINT fkivwjqk8sg0mdgepohjb5njqiv FOREIGN KEY (assignment_workflow_updatedby) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.municipality_tokens
    ADD CONSTRAINT fkj06k7ggl2cso3l8flm22ec5oo FOREIGN KEY (municipality_token_issued_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.access_control_audit
    ADD CONSTRAINT fkjmj4avnv4ij0xc1e188f7y14k FOREIGN KEY (access_control_audit_actor) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.incident_comment
    ADD CONSTRAINT fklpgdi7qyp7ou7plh8ygcl448h FOREIGN KEY (author_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.contractor_invite
    ADD CONSTRAINT fklyfuud37ux9fma29s21vllu7m FOREIGN KEY (invite_municipality_id) REFERENCES public.municipalities(municipality_id);

ALTER TABLE ONLY public.image
    ADD CONSTRAINT fkpq4es8jytaavevdrimamqmeem FOREIGN KEY (image_incident_id) REFERENCES public.incident(incident_id);

ALTER TABLE ONLY public.municipalities
    ADD CONSTRAINT fkrm3266eh0xyj11lu8nc1ee5yk FOREIGN KEY (municipality_created_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkrnctccepsvko7k3odxg26cnp6 FOREIGN KEY (municipality_id) REFERENCES public.municipalities(municipality_id);

ALTER TABLE ONLY public.dashcam_device
    ADD CONSTRAINT fksi6nnepuvyi0w790vuxrgpyaw FOREIGN KEY (device_user_id) REFERENCES public.users(user_id);
