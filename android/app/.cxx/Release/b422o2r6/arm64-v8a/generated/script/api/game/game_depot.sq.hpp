/*
 * This file is part of OpenTTD.
 * OpenTTD is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2.
 * OpenTTD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with OpenTTD. If not, see <https://www.gnu.org/licenses/old-licenses/gpl-2.0>.
 */

/**
 * @file game_depot.sq.hpp Exposes ottd c++ api to squirrel.
 * @attention THIS FILE IS AUTO-GENERATED. PLEASE DO NOT ALTER MANUALLY.
 */


#include "../script_depot.hpp"
#include "../template/template_depot.sq.hpp"


template <> SQInteger PushClassName<ScriptDepot, ScriptType::GS>(HSQUIRRELVM vm) { sq_pushstring(vm, "GSDepot"); return 1; }

void SQGSDepot_Register(Squirrel &engine)
{
	DefSQClass<ScriptDepot, ScriptType::GS> SQGSDepot("GSDepot");
	SQGSDepot.PreRegister(engine, "GSObject");

	SQGSDepot.DefSQConst(engine, ScriptDepot::ERR_DEPOT_BASE, "ERR_DEPOT_BASE");
	SQGSDepot.DefSQStaticMethod(engine, &ScriptDepot::GetName, "GetName", ".i");
	SQGSDepot.DefSQStaticMethod(engine, &ScriptDepot::SetName, "SetName", ".i.");

	SQGSDepot.PostRegister(engine);
}
