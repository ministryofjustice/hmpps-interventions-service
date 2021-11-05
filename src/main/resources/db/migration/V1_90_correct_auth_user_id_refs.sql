do 
$$
declare
    sp_user record;
begin
	raise notice 'Migration to correct auth user id references - Started';
    for sp_user in
		select au.id id, au_d.id id_deleted from auth_user au 
		inner join auth_user au_d on au_d.user_name = au.user_name and au_d.deleted is true and au.deleted is false
		where au.auth_source = 'auth'
	loop
		raise notice 'Processing auth user id:, % deleted id: %', sp_user.id, sp_user.id_deleted;
		
		update referral set sent_by_id = sp_user.id where sent_by_id = sp_user.id_deleted;
		update referral set created_by_id = sp_user.id where created_by_id = sp_user.id_deleted;
		update referral set deprecated_assigned_by_id = sp_user.id where deprecated_assigned_by_id = sp_user.id_deleted;
		update referral set deprecated_assigned_to_id = sp_user.id where deprecated_assigned_to_id = sp_user.id_deleted;
		update referral set end_requested_by_id = sp_user.id where end_requested_by_id = sp_user.id_deleted;
		
		update referral_assignments set assigned_by_id = sp_user.id where assigned_by_id = sp_user.id_deleted;
		update referral_assignments set assigned_to_id = sp_user.id where assigned_to_id = sp_user.id_deleted;
		
		update action_plan set created_by_id = sp_user.id where created_by_id = sp_user.id_deleted;
		update action_plan set submitted_by_id = sp_user.id where submitted_by_id = sp_user.id_deleted;
		update action_plan set approved_by_id = sp_user.id where approved_by_id = sp_user.id_deleted;
		
		update appointment set created_by_id = sp_user.id where created_by_id = sp_user.id_deleted;
		update appointment set appointment_feedback_submitted_by_id = sp_user.id where appointment_feedback_submitted_by_id = sp_user.id_deleted;
		update appointment set attendance_behaviour_submitted_by_id = sp_user.id where attendance_behaviour_submitted_by_id = sp_user.id_deleted;
		update appointment set attendance_submitted_by_id = sp_user.id where attendance_submitted_by_id = sp_user.id_deleted;
		
		update case_note set sent_by_id = sp_user.id where sent_by_id = sp_user.id_deleted;

		update end_of_service_report set created_by_id = sp_user.id where created_by_id = sp_user.id_deleted;
		update end_of_service_report set submitted_by_id = sp_user.id where submitted_by_id = sp_user.id_deleted;

        update deprecated_action_plan_appointment set created_by_id = sp_user.id where created_by_id = sp_user.id_deleted;

        perform pg_sleep(1); 
	end loop;
	raise notice 'Migration to correct auth user id references - Completed';
end;
$$;
